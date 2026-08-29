#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dlfcn.h>
#include <unistd.h>
#include <mach-o/dyld.h>
#include <limits.h>

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
        fprintf(stderr, "dlopen failed for %s: %s\n", jli_path, dlerror());
        return 1;
    }
    
    JLI_Launch_t jli_launch = (JLI_Launch_t)dlsym(lib, "JLI_Launch");
    if (!jli_launch) {
        fprintf(stderr, "dlsym JLI_Launch failed: %s\n", dlerror());
        return 1;
    }
    
    char jar_path[PATH_MAX];
    snprintf(jar_path, sizeof(jar_path), "%s/Contents/Resources/Java/vavi-apps-hub-0.0.6-SNAPSHOT-runnable.jar", bundle_path);
    
    const char *java_args[] = {
        exe_path,
        "-cp",
        jar_path,
        "--add-opens",
        "java.desktop/com.apple.eawt=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.lang=ALL-UNNAMED",
        "-Djna.library.path=/usr/local/lib:.",
        "-Dapple.laf.useScreenMenuBar=true",
        "-Djava.util.logging.config.file=./logging.properties",
        "vavi.apps.hub.Main"
    };
    int num_args = sizeof(java_args) / sizeof(java_args[0]);
    
    return jli_launch(num_args, (char **)java_args, 0, NULL, 0, NULL, "", "", "java", "java", 0, 0, 0, 0);
}
