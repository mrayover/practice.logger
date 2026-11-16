#pragma once
#include <cmath>
#include <algorithm>
// RBJ biquads, https://www.w3.org/TR/audio-eq-cookbook/ . 48 kHz, Q=.7071, shelf S=1.
// Coefficient targets change only on control commands; 10ms linear ramps suppress steps.
struct ChannelDsp {
    struct Biquad {
        double c[5]{1,0,0,0,0}, target[5]{1,0,0,0,0}, step[5]{};
        double x1[2]{},x2[2]{},y1[2]{},y2[2]{};
        int remaining=0;
        void reset() {
            for(int i=0;i<5;i++) c[i]=target[i]; remaining=0;
            for(int s=0;s<2;s++) x1[s]=x2[s]=y1[s]=y2[s]=0;
        }
        void set(int band,float db) {
            double next[5]{1,0,0,0,0};
            if(db!=0) {
                const double freq[4]{100,450,2000,8000};
                double a=std::pow(10.,db/40.),w=6.283185307179586*freq[band]/48000.,cs=std::cos(w),sn=std::sin(w);
                double alpha=sn/(2*.7071067811865476),beta=2*std::sqrt(a)*alpha;
                double b0,b1,b2,a0,a1,a2;
                if(band==0) {
                    b0=a*((a+1)-(a-1)*cs+beta); b1=2*a*((a-1)-(a+1)*cs); b2=a*((a+1)-(a-1)*cs-beta);
                    a0=(a+1)+(a-1)*cs+beta; a1=-2*((a-1)+(a+1)*cs); a2=(a+1)+(a-1)*cs-beta;
                } else if(band==3) {
                    b0=a*((a+1)+(a-1)*cs+beta); b1=-2*a*((a-1)+(a+1)*cs); b2=a*((a+1)+(a-1)*cs-beta);
                    a0=(a+1)-(a-1)*cs+beta; a1=2*((a-1)-(a+1)*cs); a2=(a+1)-(a-1)*cs-beta;
                } else {
                    b0=1+alpha*a; b1=-2*cs; b2=1-alpha*a; a0=1+alpha/a; a1=-2*cs; a2=1-alpha/a;
                }
                next[0]=b0/a0;next[1]=b1/a0;next[2]=b2/a0;next[3]=a1/a0;next[4]=a2/a0;
            }
            for(int i=0;i<5;i++) { target[i]=next[i];step[i]=(target[i]-c[i])/480.; } remaining=480;
        }
        void advance() {
            if(remaining>0) { for(int i=0;i<5;i++) c[i]+=step[i]; if(--remaining==0) for(int i=0;i<5;i++) c[i]=target[i]; }
        }
        double sample(double x,int s) {
            double y=c[0]*x+c[1]*x1[s]+c[2]*x2[s]-c[3]*y1[s]-c[4]*y2[s];
            if(!std::isfinite(y)) { reset();return 0; }
            y=std::clamp(y,-16.,16.); if(std::abs(y)<1.e-24) y=0;
            x2[s]=x1[s]; x1[s]=x; y2[s]=y1[s]; y1[s]=y; return y;
        }
    } bands[4];
    float gains[4]{}; bool enabled=true;
    double width=1,widthTarget=1,widthStep=0,wet=1,wetTarget=1,wetStep=0;
    int ramp=0;
    void reset() { for(auto& b:bands) b.reset();width=widthTarget;wet=wetTarget;ramp=0; }
    void set(const float* db,bool on,float spread) {
        for(int b=0;b<4;b++) if(std::isfinite(db[b])) {
            float g=std::clamp(db[b],-12.f,12.f);
            if(g!=gains[b]) { gains[b]=g;bands[b].set(b,g); }
        }
        if(!std::isfinite(spread)) spread=1;
        spread=std::clamp(spread,0.f,2.f);
        if(on!=enabled || spread!=widthTarget) {
            enabled=on;wetTarget=on?1:0;widthTarget=spread;
            wetStep=(wetTarget-wet)/480.;widthStep=(widthTarget-width)/480.;ramp=480;
        }
    }
    void frame(float& left,float& right) {
        if(ramp>0) { wet+=wetStep;width+=widthStep;if(--ramp==0) {wet=wetTarget;width=widthTarget;} }
        const double l=std::isfinite(left)?left:0,r=std::isfinite(right)?right:0;
        double a=l,b=r;
        for(auto& filter:bands) { filter.advance();a=filter.sample(a,0);b=filter.sample(b,1); }
        // Keep the dry signal exact at bypass and neutral; continue warming wet state for A/B.
        a=l+wet*(a-l);b=r+wet*(b-r);
        // Identity at 100%; mono remains mono for every width.
        double side=(a-b)*.5*(width-1);
        left=static_cast<float>(std::clamp(a+side,-16.,16.));right=static_cast<float>(std::clamp(b-side,-16.,16.));
    }
};
