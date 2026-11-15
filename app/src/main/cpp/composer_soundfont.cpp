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
    // Extend melodic edge zones without altering bank bytes or stored pitches.
    // Extreme MIDI notes transpose the nearest sampled zone; quality is source-limited.
    if(bank!=8) {
        auto& preset=f->presets[0]; int low=127,high=0;
        for(int i=0;i<preset.regionNum;i++) { low=std::min(low,(int)preset.regions[i].lokey); high=std::max(high,(int)preset.regions[i].hikey); }
        for(int i=0;i<preset.regionNum;i++) { auto& r=preset.regions[i]; if(r.lokey==low) r.lokey=0; if(r.hikey==high) r.hikey=127; }
    }
    tsf_set_output(f,TSF_STEREO_INTERLEAVED,48000,-12.0f);
    if(!tsf_set_max_voices(f,128)) { tsf_close(f); return false; }
    for(int c=0;c<16;c++) if(!tsf_channel_set_presetindex(f,c,tsf_get_presetindex(f,0,0))) { tsf_close(f); return false; }
    engine(h)->banks[bank]=f;
    return true;
}
extern "C" JNIEXPORT jlong JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_on(JNIEnv*,jobject,jlong h,jint bank,jint channel,jint pitch,jint velocity) {
    auto f=engine(h)->banks[bank];
    if(bank==8) {
        // FreePats' source kit is chromatic 48–66. Expose stable GM percussion keys.
        switch(pitch) {
            case 36: pitch=48;break; case 35: pitch=49;break;
            case 38: pitch=50;break; case 40: pitch=51;break;
            case 42: pitch=52;break; case 44: pitch=53;break; case 46: pitch=54;break;
            case 49: pitch=55;break; case 51: pitch=56;break;
            case 41: pitch=57;break; case 43: pitch=58;break;
            case 45: pitch=59;break; case 47: pitch=60;break;
            case 48: pitch=61;break; case 50: pitch=62;break;
            case 39: pitch=63;break; case 70: pitch=64;break;
            case 82: pitch=65;break; case 75: pitch=66;break;
            default: return -1;
        }
    }
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
