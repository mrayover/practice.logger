#include <jni.h>
#include <cmath>
#include <algorithm>
#include <cstring>
#define TSF_IMPLEMENTATION
#include "tsf.h"

// All access, including destruction, belongs to ComposerAudio's worker.
struct Engine { tsf* banks[9]{}; float mix[512]{}; };
static Engine* engine(jlong h) { return reinterpret_cast<Engine*>(h); }
extern "C" JNIEXPORT jlong JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_create(JNIEnv*,jobject) { return reinterpret_cast<jlong>(new Engine()); }
extern "C" JNIEXPORT jboolean JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_load(JNIEnv* env,jobject,jlong h,jint bank,jbyteArray bytes) {
    if(bank<0 || bank>=9) return false;
    auto data=env->GetByteArrayElements(bytes,nullptr);
    auto f=tsf_load_memory(data,env->GetArrayLength(bytes));
    env->ReleaseByteArrayElements(bytes,data,JNI_ABORT);
    if(!f) return false;
    tsf_set_output(f,TSF_STEREO_INTERLEAVED,48000,-12.0f);
    if(!tsf_set_max_voices(f,128)) { tsf_close(f); return false; }
    for(int c=0;c<16;c++) if(!tsf_channel_set_presetindex(f,c,tsf_get_presetindex(f,0,0))) { tsf_close(f); return false; }
    engine(h)->banks[bank]=f;
    return true;
}
extern "C" JNIEXPORT jlong JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_on(JNIEnv*,jobject,jlong h,jint bank,jint channel,jint pitch,jint velocity) {
    auto f=engine(h)->banks[bank];
    // Pin the TSF play index so overlapping identical pitches release independently.
    unsigned int id=f->voicePlayIndex;
    tsf_channel_note_on(f,channel,pitch,velocity/127.0f);
    return static_cast<jlong>(id);
}
extern "C" JNIEXPORT void JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_off(JNIEnv*,jobject,jlong h,jint bank,jlong id) {
    auto f=engine(h)->banks[bank];
    for(int i=0;i<f->voiceNum;i++) { auto& v=f->voices[i]; if(v.playingPreset!=-1 && v.playIndex==static_cast<unsigned int>(id) && v.ampenv.segment<TSF_SEGMENT_RELEASE) tsf_voice_end(f,&v); }
}
extern "C" JNIEXPORT void JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_clear(JNIEnv*,jobject,jlong h,jint first,jint last) {
    for(auto f:engine(h)->banks) if(f) for(int i=0;i<f->voiceNum;i++) { auto& v=f->voices[i]; if(v.playingChannel>=first && v.playingChannel<=last) v.playingPreset=-1; }
}
extern "C" JNIEXPORT void JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_volume(JNIEnv*,jobject,jlong h,jint channel,jfloat gain) {
    for(auto f:engine(h)->banks) if(f) tsf_channel_set_volume(f,channel,gain);
}
extern "C" JNIEXPORT jint JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_voices(JNIEnv*,jobject,jlong h) {
    int n=0; for(auto f:engine(h)->banks) if(f) n+=tsf_active_voice_count(f); return n;
}
extern "C" JNIEXPORT void JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_render(JNIEnv* env,jobject,jlong h,jshortArray output,jint offset,jint frames) {
    auto e=engine(h);
    if(frames<1 || frames>256) return;
    std::memset(e->mix,0,frames*2*sizeof(float));
    for(auto f:e->banks) if(f) tsf_render_float(f,e->mix,frames,1);
    jshort pcm[512];
    for(int i=0;i<frames*2;i++) pcm[i]=static_cast<jshort>(std::lrint(std::max(-1.0f,std::min(1.0f,e->mix[i]))*32767.0f));
    env->SetShortArrayRegion(output,offset*2,frames*2,pcm);
}
extern "C" JNIEXPORT void JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_destroy(JNIEnv*,jobject,jlong h) {
    auto e=engine(h); for(auto f:e->banks) if(f) tsf_close(f); delete e;
}
