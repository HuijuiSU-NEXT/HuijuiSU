// KernelSU 授权 ioctl 的 JNI 实现。
//
// 管理器给某个应用授予 root，需要走内核的 app_profile 通道：
//   1. syscall(SYS_reboot, 0xDEADBEEF, 0xCAFEBABE, 0, &fd) 拿到驱动 fd
//   2. ioctl(fd, KSU_IOCTL_SET_APP_PROFILE, &profile)
//
// 这里只做「默认 root 模板」的授权/撤销，结构体定义与 uapi/app_profile.h 一致。

#include <jni.h>
#include <stdbool.h>
#include <stdint.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/syscall.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <dirent.h>
#include <errno.h>

#define KSU_INSTALL_MAGIC1 0xDEADBEEF
#define KSU_INSTALL_MAGIC2 0xCAFEBABE

// _IOC(_IOC_WRITE, 'K', 12, 0) = (1<<30) | (12<<8) | 'K'
#define KSU_IOCTL_SET_APP_PROFILE (0x40000000u | (12u << 8) | 'K')

#define KSU_APP_PROFILE_VER 4
#define KSU_MAX_PACKAGE_NAME 256
#define KSU_SELINUX_DOMAIN 64
#define KSU_MAX_GROUPS 32

struct root_profile {
    int32_t uid;
    int32_t gid;
    uint32_t groups_count;
    int32_t groups[KSU_MAX_GROUPS];
    struct {
        uint64_t effective;
        uint64_t permitted;
        uint64_t inheritable;
    } capabilities;
    char selinux_domain[KSU_SELINUX_DOMAIN];
    int32_t namespaces;
    uint64_t flags;
};

struct non_root_profile {
    bool umount_modules;
};

struct app_profile {
    uint32_t version;
    char key[KSU_MAX_PACKAGE_NAME];
    int32_t curr_uid;
    bool allow_su;
    union {
        struct {
            bool use_default;
            char template_name[KSU_MAX_PACKAGE_NAME];
            struct root_profile profile;
        } rp_config;
        struct {
            bool use_default;
            struct non_root_profile profile;
        } nrp_config;
    };
};

static int g_driver_fd = -1;

// 只在已加载 KernelSU 的进程里能找到这个匿名 fd（名字由内核设置为 [ksu_driver]）。
// 先扫描 /proc/self/fd，扫不到就直接返回 -1，不去触发 syscall，
// 避免在普通设备上调用 reboot 钩子导致进程崩溃。
static int scan_driver_fd(void) {
    DIR *d = opendir("/proc/self/fd");
    if (!d) return -1;
    int found = -1;
    struct dirent *e;
    char path[64];
    char link[256];
    while ((e = readdir(d)) != NULL) {
        if (e->d_name[0] == '.') continue;
        snprintf(path, sizeof(path), "/proc/self/fd/%s", e->d_name);
        ssize_t n = readlink(path, link, sizeof(link) - 1);
        if (n <= 0) continue;
        link[n] = 0;
        if (strstr(link, "ksu_driver") != NULL) {
            found = atoi(e->d_name);
            break;
        }
    }
    closedir(d);
    return found;
}

static int get_driver_fd(void) {
    if (g_driver_fd >= 0) return g_driver_fd;
    int fd = scan_driver_fd();
    if (fd < 0) return -1;
    g_driver_fd = fd;
    return fd;
}

// 返回 0 成功，否则 -errno（或 -100 表示拿不到驱动 fd）。
JNIEXPORT jint JNICALL
Java_com_huijui_nova_native_KsuNative_setAppProfile(JNIEnv *env, jclass clazz,
                                                    jint uid, jstring pkg, jboolean allow) {
    (void) clazz;
    int fd = get_driver_fd();
    if (fd < 0) return -100;

    struct app_profile p;
    memset(&p, 0, sizeof(p));
    p.version = KSU_APP_PROFILE_VER;
    p.curr_uid = uid;
    p.allow_su = allow ? true : false;

    const char *name = (*env)->GetStringUTFChars(env, pkg, NULL);
    if (name == NULL) return -EINVAL;
    strncpy(p.key, name, KSU_MAX_PACKAGE_NAME - 1);
    (*env)->ReleaseStringUTFChars(env, pkg, name);

    if (allow) {
        p.rp_config.use_default = true;
        strncpy(p.rp_config.profile.selinux_domain, "u:r:su:s0", KSU_SELINUX_DOMAIN - 1);
        p.rp_config.profile.namespaces = -1;
    } else {
        p.nrp_config.use_default = true;
    }

    int ret = ioctl(fd, KSU_IOCTL_SET_APP_PROFILE, &p);
    if (ret < 0) return -errno;
    return 0;
}

// 读驱动版本号，用于判断内核是否在工作（不依赖 root）。
JNIEXPORT jint JNICALL
Java_com_huijui_nova_native_KsuNative_driverAlive(JNIEnv *env, jclass clazz) {
    (void) env;
    (void) clazz;
    return get_driver_fd() >= 0 ? 1 : 0;
}
