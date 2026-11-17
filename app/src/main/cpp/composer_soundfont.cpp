#include <jni.h>
#include <cmath>
#include <algorithm>
#include <cstring>
#define TSF_IMPLEMENTATION
#include "tsf.h"
#include "channel_dsp.h"

// All access, including destruction, belongs to ComposerAudio's worker.
struct Shape { bool enabled=false; float attack=10,release=300,cutoff=18000,q=.707f,sustain=.8f,pan=0; };
struct Engine { ChannelDsp processing[16]; tsf* banks[9]{}; float mix[512]{}; float channels[16][512]{}; float panStay[16]; float panCross[16]{}; Engine() { std::fill_n(panStay,16,1.f); } Shape shapes[16]{}; tsf_region regions[9][128]{}; };
// Each native voice gets a private region. Shared preset/sample data is never edited.
static void shapeVoice(Engine* e,int bank,tsf_voice& v,int slot,bool fresh) {
    const auto& s=e->shapes[v.playingChannel]; if(!s.enabled) return;
    auto f=e->banks[bank]; auto& r=e->regions[bank][slot];
    if(fresh) { r=*v.region; v.region=&r; }
    r.modEnvToFilterFc=0; r.modLfoToFilterFc=0;
    r.initialFilterFc=static_cast<int>(1200*std::log2(s.cutoff/8.176f));
    r.initialFilterQ=static_cast<int>(200*std::log10(s.q));
    r.ampenv.attack=s.attack/1000; r.ampenv.release=s.release/1000;
    r.ampenv.delay=0; r.ampenv.hold=0; r.ampenv.decay=.3f; r.ampenv.sustain=s.sustain;
    r.ampenv.keynumToHold=0; r.ampenv.keynumToDecay=0;
    if(fresh) tsf_voice_envelope_setup(&v.ampenv,&r.ampenv,v.playingKey,v.ampenv.midiVelocity,TSF_TRUE,f->outSampleRate);
    else {
        v.ampenv.parameters.release=r.ampenv.release; v.ampenv.parameters.sustain=s.sustain;
        if(v.ampenv.segment==TSF_SEGMENT_SUSTAIN) v.ampenv.level=s.sustain;
    }
    v.lowpass.QInv=1.0/s.q; v.lowpass.active=TSF_TRUE;
    tsf_voice_lowpass_setup(&v.lowpass,s.cutoff/f->outSampleRate);
    v.panFactorLeft=std::sqrt((1-s.pan)*.5f); v.panFactorRight=std::sqrt((1+s.pan)*.5f);
}
static Engine* engine(jlong h) { return reinterpret_cast<Engine*>(h); }
// Read installed factory envelopes and custom channel releases, in seconds.
extern "C" JNIEXPORT jfloat JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_releaseBound(JNIEnv*,jobject,jlong h) {
    auto e=engine(h); float seconds=TSF_FASTRELEASETIME;
    for(auto f:e->banks) if(f) for(int p=0;p<f->presetNum;p++)
        for(int r=0;r<f->presets[p].regionNum;r++) seconds=std::max(seconds,f->presets[p].regions[r].ampenv.release);
    for(const auto& s:e->shapes) if(s.enabled) seconds=std::max(seconds,s.release/1000.f);
    return seconds;
}
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
    for(int i=0;i<f->voiceNum;i++) { auto& v=f->voices[i]; if(v.playingPreset!=-1 && v.playIndex==id) shapeVoice(engine(h),bank,v,i,true); }
    return static_cast<jlong>(id);
}
extern "C" JNIEXPORT void JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_off(JNIEnv*,jobject,jlong h,jint bank,jlong id) {
    auto f=engine(h)->banks[bank];
    for(int i=0;i<f->voiceNum;i++) { auto& v=f->voices[i]; if(v.playingPreset!=-1 && v.playIndex==static_cast<unsigned int>(id) && v.ampenv.segment<TSF_SEGMENT_RELEASE) tsf_voice_end(f,&v); }
}
extern "C" JNIEXPORT void JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_clear(JNIEnv*,jobject,jlong h,jint first,jint last) {
    for(auto f:engine(h)->banks) if(f) for(int i=0;i<f->voiceNum;i++) { auto& v=f->voices[i]; if(v.playingChannel>=first && v.playingChannel<=last) v.playingPreset=-1; }
    for(int c=std::max(0,first);c<=std::min(15,last);c++) engine(h)->processing[c].reset();
}
extern "C" JNIEXPORT void JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_volume(JNIEnv*,jobject,jlong h,jint channel,jfloat gain) {
    if(channel<0 || channel>15 || !std::isfinite(gain)) return;
    if(gain==0) engine(h)->processing[channel].reset();
    for(auto f:engine(h)->banks) if(f) tsf_channel_set_volume(f,channel,gain);
}
extern "C" JNIEXPORT jint JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_voices(JNIEnv*,jobject,jlong h) {
    int n=0; for(auto f:engine(h)->banks) if(f) n+=tsf_active_voice_count(f); return n;
}
extern "C" JNIEXPORT void JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_render(JNIEnv* env,jobject,jlong h,jshortArray output,jint offset,jint frames) {
    auto e=engine(h);
    if(frames<1 || frames>256) return;
    std::memset(e->mix,0,frames*2*sizeof(float));
    // Same traversal as tsf_render_float: every active layer advances exactly once.
    // Sequence 0..7 and preview 8..15 stay separate (their UUID order can differ).
    for(auto& channel:e->channels) std::memset(channel,0,frames*2*sizeof(float));
    for(auto f:e->banks) if(f) for(int i=0;i<f->voiceNum;i++) {
        auto& v=f->voices[i];
        if(v.playingPreset!=-1) tsf_voice_render(f,&v,e->channels[v.playingChannel],frames);
    }
    for(int c=0;c<16;c++) for(int i=0;i<frames*2;i+=2) {
        float l=e->channels[c][i],r=e->channels[c][i+1];
        e->processing[c].frame(l,r);
        const float cross=e->panCross[c],stay=e->panStay[c];
        // Stereo pan matrix: center is identity; at either extreme both source
        // components reach that side. Each moved component uses equal-power gains.
        e->mix[i] += cross>=0 ? stay*l : l-cross*r;
        e->mix[i+1] += cross>=0 ? r+cross*l : stay*r;
    }
    jshort pcm[512];
    for(int i=0;i<frames*2;i++) pcm[i]=static_cast<jshort>(std::lrint(std::max(-1.0f,std::min(1.0f,e->mix[i]))*32767.0f));
    env->SetShortArrayRegion(output,offset*2,frames*2,pcm);
}
extern "C" JNIEXPORT void JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_destroy(JNIEnv*,jobject,jlong h) {
    auto e=engine(h); for(auto f:e->banks) if(f) tsf_close(f); delete e;
}

extern "C" JNIEXPORT void JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_shape(JNIEnv*,jobject,jlong h,jint channel,jboolean enabled,jfloat attack,jfloat release,jfloat cutoff,jfloat q,jfloat sustain,jfloat pan) {
    if(channel<0 || channel>15 || !std::isfinite(attack) || !std::isfinite(release) || !std::isfinite(cutoff) || !std::isfinite(q) || !std::isfinite(sustain) || !std::isfinite(pan)) return;
    auto e=engine(h); auto& s=e->shapes[channel]; bool wasEnabled=s.enabled;
    s={enabled==JNI_TRUE,std::clamp(attack,0.f,3000.f),std::clamp(release,10.f,5000.f),std::clamp(cutoff,40.f,18000.f),std::clamp(q,.5f,4.f),std::clamp(sustain,0.f,1.f),std::clamp(pan,-1.f,1.f)};
    for(int b=0;b<9;b++) if(auto f=e->banks[b]) for(int i=0;i<f->voiceNum;i++) {
        auto& v=f->voices[i]; if(v.playingPreset==-1 || v.playingChannel!=channel) continue;
        // Switching back to untouched factory state releases preview rather than mutating regions.
        if(!s.enabled || !wasEnabled) v.playingPreset=-1;
        else shapeVoice(e,b,v,i,false);
    }
}

extern "C" JNIEXPORT void JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_pan(JNIEnv*,jobject,jlong h,jint channel,jfloat value) {
    if(channel<0 || channel>15 || !std::isfinite(value)) return;
    const float p=std::clamp(value,-1.f,1.f), angle=std::abs(p)*1.57079632679f;
    auto e=engine(h); e->panStay[channel]=std::abs(p)==1 ? 0.f : std::cos(angle);
    e->panCross[channel]=std::copysign(std::sin(angle),p);
}

extern "C" JNIEXPORT void JNICALL Java_com_mrayover_practicelogger_composer_ComposerSynth_processing(JNIEnv*,jobject,jlong h,jint channel,jfloat low,jfloat lowMid,jfloat highMid,jfloat high,jboolean enabled,jfloat width) {
    if(channel<0 || channel>15) return;
    const float db[4]{low,lowMid,highMid,high};engine(h)->processing[channel].set(db,enabled==JNI_TRUE,width);
}
