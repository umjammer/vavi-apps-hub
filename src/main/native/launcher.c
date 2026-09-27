#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dlfcn.h>
#include <unistd.h>
#include <mach-o/dyld.h>
#include <limits.h>
#include <dirent.h>

typedef int (*JLI_Launch_t)(int argc, char ** argv,
                            int jargc, const char** jargv,
                            int appclassc, const char** appclassv,
                            const char* fullversion,
                            const char* dotversion,
                            const char* pname,
                            const char* lname,
                            int javaargs,
                            int cpwildcard,
                            int javaw,
                            int ergo);

static int get_java_home(char *out, size_t maxlen) {
    const char *env_home = getenv("JAVA_HOME");
    if (env_home && strlen(env_home) > 0) {
        strncpy(out, env_home, maxlen - 1);
        out[maxlen - 1] = '\0';
        return 0;
    }
    FILE *fp = popen("/usr/libexec/java_home -v 17+", "r");
    if (!fp) return -1;
    if (fgets(out, maxlen, fp) != NULL) {
        size_t len = strlen(out);
        while (len > 0 && (out[len - 1] == '\n' || out[len - 1] == '\r')) {
            out[--len] = '\0';
        }
        pclose(fp);
        return 0;
    }
    pclose(fp);
    return -1;
}

static int find_jar_path(const char *java_dir, char *out_jar_path, size_t maxlen) {
    DIR *dir = opendir(java_dir);
    if (!dir) return -1;

    struct dirent *entry;
    char candidate[PATH_MAX] = {0};

    while ((entry = readdir(dir)) != NULL) {
        if (entry->d_name[0] == '.') continue;
        size_t name_len = strlen(entry->d_name);
        if (name_len > 4 && strcmp(entry->d_name + name_len - 4, ".jar") == 0) {
            // Prioritize *-runnable.jar if present
            if (strstr(entry->d_name, "-runnable.jar") != NULL) {
                snprintf(out_jar_path, maxlen, "%s/%s", java_dir, entry->d_name);
                closedir(dir);
                return 0;
            }
            if (candidate[0] == '\0') {
                snprintf(candidate, sizeof(candidate), "%s/%s", java_dir, entry->d_name);
            }
        }
    }
    closedir(dir);

    if (candidate[0] != '\0') {
        strncpy(out_jar_path, candidate, maxlen - 1);
        out_jar_path[maxlen - 1] = '\0';
        return 0;
    }
    return -1;
}

int main(int argc, char *argv[]) {
    char exe_path[PATH_MAX];
    uint32_t size = sizeof(exe_path);
    if (_NSGetExecutablePath(exe_path, &size) != 0) {
        return 1;
    }
    char bundle_path[PATH_MAX];
    strncpy(bundle_path, exe_path, sizeof(bundle_path));
    char *p = strstr(bundle_path, "/Contents/MacOS/");
    if (!p) return 1;
    *p = '\0';
    
    char resources_path[PATH_MAX];
    snprintf(resources_path, sizeof(resources_path), "%s/Contents/Resources", bundle_path);
    chdir(resources_path);
    
    char java_home[PATH_MAX];
    if (get_java_home(java_home, sizeof(java_home)) != 0) {
        fprintf(stderr, "Cannot find Java\n");
        return 1;
    }
    
    char jli_path[PATH_MAX];
    snprintf(jli_path, sizeof(jli_path), "%s/lib/libjli.dylib", java_home);
    void *lib = dlopen(jli_path, RTLD_NOW | RTLD_GLOBAL);
    if (!lib) {
        snprintf(jli_path, sizeof(jli_path), "%s/lib/jli/libjli.dylib", java_home);
        lib = dlopen(jli_path, RTLD_NOW | RTLD_GLOBAL);
    }
    if (!lib) {
        fprintf(stderr, "dlopen failed for libjli.dylib: %s\n", dlerror());
        return 1;
    }
    
    JLI_Launch_t jli_launch = (JLI_Launch_t)dlsym(lib, "JLI_Launch");
    if (!jli_launch) {
        fprintf(stderr, "dlsym JLI_Launch failed: %s\n", dlerror());
        return 1;
    }
    
    char java_dir[PATH_MAX];
    snprintf(java_dir, sizeof(java_dir), "%s/Contents/Resources/Java", bundle_path);
    char jar_path[PATH_MAX];
    if (find_jar_path(java_dir, jar_path, sizeof(jar_path)) != 0) {
        fprintf(stderr, "Cannot find runnable JAR in %s\n", java_dir);
        return 1;
    }
    
    const char *base_args[] = {
        exe_path,
        "-cp",
        jar_path,
        "--add-opens",
        "java.desktop/com.apple.eawt=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.lang=ALL-UNNAMED",
        "-Djna.library.path=/opt/homebrew/lib:/usr/local/lib:.",
        "-Dapple.laf.useScreenMenuBar=true",
        "-Djava.util.logging.config.file=./logging.properties",
        // keep in sync with vmArgs of javapackager in pom.xml, Info.plist VMOptions are not used by this launcher
        "-Dvavi.games.input.hid4java.darwinOpenDevicesNonExclusive=true",
        "-Dvavi.games.input.hid4java.usages=0x01:0x05,0x01:0x06",
        "vavi.apps.hub.Main"
    };
    int base_count = sizeof(base_args) / sizeof(base_args[0]);
    int extra_count = (argc > 1) ? (argc - 1) : 0;
    int total_args = base_count + extra_count;

    const char **java_args = (const char **)malloc(sizeof(char *) * total_args);
    if (!java_args) return 1;

    for (int i = 0; i < base_count; i++) {
        java_args[i] = base_args[i];
    }
    for (int i = 0; i < extra_count; i++) {
        java_args[base_count + i] = argv[1 + i];
    }
    
    return jli_launch(total_args, (char **)java_args, 0, NULL, 0, NULL, "", "", "java", "java", 0, 0, 0, 0);
}
