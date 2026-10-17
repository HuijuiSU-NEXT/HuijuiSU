package com.huijui.sunext.data.profile

import com.huijui.sunext.Natives
import com.huijui.sunext.domain.model.AppProfile

internal fun Natives.Profile.toDomain(): AppProfile = AppProfile(
    name = this.name,
    currentUid = this.currentUid,
    allowSu = this.allowSu,
    rootUseDefault = this.rootUseDefault,
    rootTemplate = this.rootTemplate,
    uid = this.uid,
    gid = this.gid,
    groups = this.groups,
    capabilities = this.capabilities,
    ctx = this.ctx, // 注意：Natives 里叫 ctx，AppProfile 里叫 context
    namespace = this.namespace,
    nonRootUseDefault = this.nonRootUseDefault,
    umountModules = this.umountModules,
    rules = this.rules,
    flags = this.flags
)

internal fun AppProfile.toNative(): Natives.Profile = Natives.Profile(
    name = this.name,
    currentUid = this.currentUid,
    allowSu = this.allowSu,
    rootUseDefault = this.rootUseDefault,
    rootTemplate = this.rootTemplate,
    uid = this.uid,
    gid = this.gid,
    groups = this.groups,
    capabilities = this.capabilities,
    ctx = this.ctx, // 注意：Natives 里叫 ctx，AppProfile 里叫 context
    namespace = this.namespace,
    nonRootUseDefault = this.nonRootUseDefault,
    umountModules = this.umountModules,
    rules = this.rules,
    flags = this.flags
)
