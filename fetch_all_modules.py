import requests, json, os, time, re
from concurrent.futures import ThreadPoolExecutor, as_completed

TOKEN = os.getenv("GITHUB_TOKEN")
HEADERS = {"Accept": "application/vnd.github.v3+json"}
if TOKEN: HEADERS["Authorization"] = f"token {TOKEN}"

# 🚨 终极防格机黑名单：发现直接封杀
BLACKLIST = [
    "format", "rm -rf", "dd if=", "wipe", "mkfs", "brick", "ransom", "miner",
    "格机", "删库", "病毒", "木马", "变砖", "cheat", "hack", "trojan", "steal"
]

def load_manual_list():
    if not os.path.exists("custom_repos.txt"): return []
    with open("custom_repos.txt", "r", encoding="utf-8") as f:
        return [line.strip() for line in f if line.strip() and not line.startswith('#')]

# 精简搜索维度，扩大覆盖面
QUERIES = [
    "topic:kernelsu", "topic:magisk-module", "topic:magisk", "topic:lsposed", 
    "topic:zygisk", "topic:susfs", "topic:gki", "topic:magisk-hide",
    "kernelsu in:name,description", "magisk in:name,description",
    "lsposed in:name,description", "zygisk in:name,description"
]

def clean_text(text):
    if not text: return "暂无描述"
    text = re.sub(r'[\r\n\t]+', ' ', text)
    text = re.sub(r'[#\*`>]', '', text)
    return text[:100].strip() + "..."

def get_tags(repo_name, desc):
    text = (repo_name + " " + (desc or "")).lower()
    tags = []
    if any(k in text for k in ["kernel", "gki", "susfs"]): tags.append("内核底层")
    if any(k in text for k in ["hide", "shamiko", "tricky", "spoof", "env"]): tags.append("环境隐藏")
    if any(k in text for k in ["zygisk", "lsposed", "xposed", "hook"]): tags.append("XP注入")
    if any(k in text for k in ["audio", "sound", "font", "emoji", "theme", "ui", "美化"]): tags.append("视觉美化")
    if any(k in text for k in ["battery", "thermal", "charge", "perf", "sched", "调度"]): tags.append("性能调度")
    if not tags: tags.append("实用工具")
    return tags

def search_repos():
    all_repos = {}
    print("🔍 开始多维话题扫描...", flush=True)
    for q in QUERIES:
        print(f"👉 {q} ...", flush=True)
        for page in range(1, 4):
            url = f"https://api.github.com/search/repositories?q={q}&sort=updated&order=desc&per_page=100&page={page}"
            try:
                resp = requests.get(url, headers=HEADERS, timeout=15)
                if resp.status_code == 200:
                    items = resp.json().get("items", [])
                    if not items: break
                    for r in items:
                        if r.get("fork", False): continue
                        if not any(w in r["full_name"].lower() or w in (r.get("description") or "").lower() for w in BLACKLIST):
                            all_repos[r["full_name"]] = r
            except Exception: pass
            time.sleep(2)
    return all_repos

def get_module_data(repo, repo_info):
    try:
        resp = requests.get(f"https://api.github.com/repos/{repo}/releases", headers=HEADERS, timeout=10)
        if resp.status_code != 200: return None
        releases_data = resp.json()
        if not releases_data: return None

        valid_releases = []
        for rel in releases_data[:3]:
            zip_asset = next((a for a in rel.get("assets", []) if a["name"].endswith(".zip")), None)
            if zip_asset:
                size_mb = zip_asset["size"] / (1024 * 1024)
                # 🚀 限制直接放宽到 1024MB (1GB)！
                if 0.001 <= size_mb <= 1024:
                    valid_releases.append({
                        "name": rel.get("tag_name", "Unknown"),
                        "publishedAt": rel.get("published_at", ""),
                        "zipUrl": zip_asset["browser_download_url"],
                        "size": zip_asset["size"],
                        "downloadCount": zip_asset.get("download_count", 0)
                    })
        if not valid_releases: return None
        
        latest = valid_releases[0]
        raw_desc = releases_data[0].get("body") or repo_info.get("description") or ""
        
        # 尝试去读 module.prop 拿真实版本
        real_ver = latest["name"]
        for branch in ["main", "master"]:
            prop_url = f"https://raw.githubusercontent.com/{repo}/{branch}/module.prop"
            try:
                prop_resp = requests.get(prop_url, timeout=5)
                if prop_resp.status_code == 200:
                    for line in prop_resp.text.splitlines():
                        if line.startswith("version="):
                            real_ver = line.split("=", 1)[1].strip()
                    break
            except Exception: pass

        return {
            "id": repo.replace("/", "_"),
            "name": repo.split("/")[1],
            "author": repo.split("/")[0],
            "version": real_ver,
            "description": clean_text(raw_desc),
            "tags": get_tags(repo, raw_desc),
            "downloadCount": latest["downloadCount"],
            "stars": repo_info.get("stargazers_count", 0),
            "updatedAt": latest["publishedAt"],
            "releases": valid_releases
        }
    except Exception: return None

def main():
    all_repos = search_repos()
    
    # 强制加入白名单
    manual_repos = load_manual_list()
    for mr in manual_repos:
        if mr not in all_repos:
            all_repos[mr] = {"description": "手动白名单强制收录"}

    print(f"\n📦 锁定 {len(all_repos)} 个候选仓库！启动 20 线程深度质检...", flush=True)
    modules = []
    
    with ThreadPoolExecutor(max_workers=20) as ex:
        futures = {ex.submit(get_module_data, repo, info): repo for repo, info in all_repos.items()}
        for i, f in enumerate(as_completed(futures)):
            rep = f.result()
            if rep:
                modules.append(rep)
    
    # 去重
    unique_mods = {}
    for m in modules:
        if m["id"] not in unique_mods or m["downloadCount"] > unique_mods[m["id"]]["downloadCount"]:
            unique_mods[m["id"]] = m
            
    final_list = list(unique_mods.values())
    final_list.sort(key=lambda x: x["updatedAt"], reverse=True)
    
    with open("modules.json", "w", encoding="utf-8") as f:
        json.dump(final_list, f, indent=2, ensure_ascii=False)
    print(f"\n🎉 终极安检完毕！共收录 {len(final_list)} 个模块！", flush=True)

if __name__ == "__main__":
    main()
