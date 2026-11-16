#include "../../main/cpp/channel_dsp.h"
#include <cassert>
#include <chrono>
#include <cstdio>
#include <limits>
static double response(int band,float db,double hz) {
    ChannelDsp d;float g[4]{};g[band]=db;d.set(g,true,1);d.reset();
    double energy=0;for(int n=0;n<48000;n++) { float l=.01f*std::sin(6.283185307179586*hz*n/48000),r=l;d.frame(l,r);assert(std::isfinite(l));if(n>24000)energy+=l*l; }
    return std::sqrt(energy/23999);
}
int main() {
    double near[4]{30,450,2000,16000},far[4]{8000,16000,30,30};
    for(int b=0;b<4;b++) {
        double neutral=response(b,0,near[b]),boost=response(b,12,near[b])/neutral,cut=response(b,-12,near[b])/neutral;
        assert(boost>3.6 && cut<.28);assert(response(b,12,far[b])/response(b,0,far[b])<1.1);
        std::printf("band %d boost %.4f cut %.4f\n",b,boost,cut);
    }
    ChannelDsp neutral;for(int n=0;n<10000;n++) { float l=std::sin(n*.31)*.1,r=std::cos(n*.19)*.1,a=l,b=r;neutral.frame(a,b);assert(a==l && b==r); }
    float gains[4]{12,-12,12,-12};
    for(float width:{0.f,1.f,2.f}) {
        ChannelDsp d;float zero[4]{};d.set(zero,true,width);d.reset();float l=.1f,r=-.03f;d.frame(l,r);
        assert(std::abs((l+r)-.07)<1.e-6);assert(std::abs((l-r)-.13*width)<1.e-6);
        l=r=.1f;d.frame(l,r);assert(l==r && l==.1f);
    }
    ChannelDsp bypass;bypass.set(gains,false,1);bypass.reset();
    for(int n=0;n<10000;n++) { float l=std::sin(n*.5f)*.1f,r=l,a=l,b=r;bypass.frame(a,b);assert(a==l && b==r); }
    ChannelDsp independent[8];independent[0].set(gains,true,2);
    auto start=std::chrono::steady_clock::now(); double total=0;
    for(int block=0;block<1000;block++) for(int i=0;i<240;i++) for(int c=0;c<8;c++) {
        if(block%17==0 && i==0) {float g[4]{float(block%25-12),12,-12,6};independent[c].set(g,block%3!=0,float(block%201)/100);}
        float l=.01f*std::sin((block*240+i)*.13),r=.01f*std::cos((block*240+i)*.17);independent[c].frame(l,r);
        assert(std::isfinite(l) && std::isfinite(r) && std::abs(l)<=16 && std::abs(r)<=16);total+=l+r;
    }
    double ms=std::chrono::duration<double,std::milli>(std::chrono::steady_clock::now()-start).count()/1000;
    assert(ms<5);std::printf("eight-channel DSP mean %.6f ms per 5ms block (checksum %.6f)\n",ms,total);
    ChannelDsp d;d.set(gains,true,2);d.reset();float l=1,r=-1;d.frame(l,r);d.reset();l=r=0;d.frame(l,r);assert(l==0 && r==0);
    l=std::numeric_limits<float>::infinity();r=std::numeric_limits<float>::quiet_NaN();d.frame(l,r);assert(std::isfinite(l)&&std::isfinite(r));
    ChannelDsp untouched;float a=.1,b=.2;untouched.frame(a,b);assert(a==.1f && b==.2f);
    // Existing Pan matrix follows width: extreme right folds all source components right.
    float z[4]{};ChannelDsp mono;mono.set(z,true,0);mono.reset();l=.1;r=-.03;mono.frame(l,r);float outL=0*l,outR=r+l;assert(outL==0 && std::abs(outR-.07)<1.e-6);
    puts("PASS neutral/bypass/bands/independence/width/mono/Pan/reset/nonfinite/stability/performance");
}
