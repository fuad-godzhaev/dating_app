#include <jni.h>
#include <string>
#include <pthread.h>
#include <android/log.h>
#include <node.h>

#define LOG_TAG "NodeBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static pthread_t node_thread;
static bool node_running = false;

struct NodeStartArgs {
    char* script_path;
    char** argv;
    int argc;
};

void* runNodeThread(void* arg) {
    NodeStartArgs* args = (NodeStartArgs*)arg;

    LOGI("Starting Node.js with script: %s", args->script_path);

    // Start Node.js event loop
    int result = node::Start(args->argc, args->argv);

    LOGI("Node.js exited with code: %d", result);

    node_running = false;

    // Cleanup
    for (int i = 0; i < args->argc; i++) {
        free(args->argv[i]);
    }
    free(args->argv);
    free(args->script_path);
    free(args);

    return nullptr;
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_apiguave_pds_NodeJsBridge_startNode(
        JNIEnv* env,
        jobject /* this */,
        jstring scriptPath) {

    if (node_running) {
        LOGE("Node.js is already running");
        return JNI_FALSE;
    }

    const char* script = env->GetStringUTFChars(scriptPath, nullptr);

    // Prepare arguments for Node.js
    NodeStartArgs* args = (NodeStartArgs*)malloc(sizeof(NodeStartArgs));
    args->argc = 2;
    args->argv = (char**)malloc(sizeof(char*) * args->argc);
    args->argv[0] = strdup("node");
    args->argv[1] = strdup(script);
    args->script_path = strdup(script);

    env->ReleaseStringUTFChars(scriptPath, script);

    // Create thread for Node.js
    int result = pthread_create(&node_thread, nullptr, runNodeThread, args);

    if (result != 0) {
        LOGE("Failed to create Node.js thread: %d", result);
        free(args->script_path);
        for (int i = 0; i < args->argc; i++) {
            free(args->argv[i]);
        }
        free(args->argv);
        free(args);
        return JNI_FALSE;
    }

    node_running = true;
    pthread_detach(node_thread);

    LOGI("Node.js thread started successfully");
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_apiguave_pds_NodeJsBridge_isNodeRunning(
        JNIEnv* env,
        jobject /* this */) {
    return node_running ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_apiguave_pds_NodeJsBridge_stopNode(
        JNIEnv* env,
        jobject /* this */) {

    if (!node_running) {
        LOGE("Node.js is not running");
        return;
    }

    // Note: Node.js doesn't have a clean way to stop from native code
    // In production, you'd implement IPC to send SIGTERM to the script
    LOGI("Stopping Node.js (graceful shutdown not fully implemented)");

    // Mark as not running
    node_running = false;
}

} // extern "C"
