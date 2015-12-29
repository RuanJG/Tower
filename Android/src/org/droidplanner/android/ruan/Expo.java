package org.droidplanner.android.ruan;

/**
 * Created by joe on 2015/12/28.
 */
public class Expo {

    private int  RESX_SHIFT = 10;
    private int  RESX     =  1024;
    private  int  RESXu    =  1024;
    private long  RESXul  =   1024;
    private long   RESXl   =   1024;
    short calc100to256_16Bits(short x) // return x*2.56
    {
        // y = 2*x + x/2 +x/16-x/512-x/2048
        // 512 and 2048 are out of scope from int8 input --> forget it
        //#ifdef CORRECT_NEGATIVE_SHIFTS
        short res=(short)(x<<1);
        //char  sign=(uchar) x>>7;
        char sign= (char) (x<0?1:0);

        x-=sign;
        res+=(x>>1);
        res+=sign;
        res+=(x>>4);
        res+=sign;
        return res;
        //#else
        //return ((short)x<<1)+(x>>1)+(x>>4);
        //#endif
    }

    short calc100to256(char x) // return x*2.56
    {
        return calc100to256_16Bits((short)x);
    }
// #define EXTENDED_EXPO
// increases range of expo curve but costs about 82 bytes flash

// expo-funktion:
// ---------------
// kmplot
// f(x,k)=exp(ln(x)*k/10) ;P[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20]
// f(x,k)=x*x*x*k/10 + x*(1-k/10) ;P[0,1,2,3,4,5,6,7,8,9,10]
// f(x,k)=x*x*k/10 + x*(1-k/10) ;P[0,1,2,3,4,5,6,7,8,9,10]
// f(x,k)=1+(x-1)*(x-1)*(x-1)*k/10 + (x-1)*(1-k/10) ;P[0,1,2,3,4,5,6,7,8,9,10]
// don't know what this above should be, just confusing in my opinion,

// here is the real explanation
// actually the real formula is
/*
 f(x) = exp( ln(x) * 10^k)
 if it is 10^k or e^k or 2^k etc. just defines the max distortion of the expo curve; I think 10 is useful
 this gives values from 0 to 1 for x and output; k must be between -1 and +1
 we do not like to calculate with floating point. Therefore we rescale for x from 0 to 1024 and for k from -100 to +100
 f(x) = 1024 * ( e^( ln(x/1024) * 10^(k/100) ) )
 This would be really hard to be calculated by such a microcontroller
 Therefore Thomas Husterer compared a few usual function something like x^3, x^4*something, which look similar
 Actually the formula
 f(x) = k*x^3+x*(1-k)
 gives a similar form and should have even advantages compared to a original exp curve.
 This function again expect x from 0 to 1 and k only from 0 to 1
 Therefore rescaling is needed like before:
 f(x) = 1024* ((k/100)*(x/1024)^3 + (x/1024)*(100-k)/100)
 some mathematical tricks
 f(x) = (k*x*x*x/(1024*1024) + x*(100-k)) / 100
 for better rounding results we add the 50
 f(x) = (k*x*x*x/(1024*1024) + x*(100-k) + 50) / 100

 because we now understand the formula, we can optimize it further
 --> calc100to256(k) --> eliminates /100 by replacing with /256 which is just a simple shift right 8
 k is now between 0 and 256
 f(x) = (k*x*x*x/(1024*1024) + x*(256-k) + 128) / 256
 */

// input parameters;
//  x 0 to 1024;
//  k 0 to 100;
// output between 0 and 1024
    int expou( int x,  int k)
    {
        /*
        #if defined(EXTENDED_EXPO)
        bool extended;
        if (k>80) {
            extended=true;
        }
        else {
            k += (k>>2);  // use bigger values before extend, because the effect is anyway very very low
            extended=false;
        }
        #endif
        */
        k = calc100to256((char) k);

        int value = (int) x*x;
        value *= (int)k;
        value >>= 8;
        value *= (int)x;

        /*
        #if defined(EXTENDED_EXPO)
        if (extended) {  // for higher values do more multiplications to get a stronger expo curve
            value >>= 16;
            value *= (int)x;
            value >>= 4;
            value *= (int)x;
        }
        #endif
        */

        value >>= 12;
        value += (int)(256-k)*x+128;

        return value>>8;
    }

    public  int expo(int x, int k)
    {
        if (k == 0) return x;
        int y;
        boolean neg = (x < 0);

        if (neg) x = -x;
        if (k<0) {
            y = RESXu-expou(RESXu-x, -k);
        }
        else {
            y = expou(x, k);
        }
        return neg? -y : y;
    }
}
