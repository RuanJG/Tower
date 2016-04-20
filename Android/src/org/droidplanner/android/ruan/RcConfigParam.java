package org.droidplanner.android.ruan;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by joe on 2015/12/31.
 */
public class RcConfigParam {
    public static class mixConfig{
        public int mainChan = -1;
        public int slaveChan = -1;
        public int persenAtAdd = 0;
        public int persenAtSub = 0;
        public int mainChanStartPoint = 1;
        public boolean valiable = false;
        public mixConfig(){ };
        public boolean isValiable(){
            if( mainChan >= 0 && slaveChan >=0 && valiable)
                return true;
            return false;
        }
    }
    public static class baseConfig{
        public short id = -1;
        public boolean revert = false;
        public short trim = 0;
        public short minValue = 1000;
        public short maxValue = 2000;
        public short curveType = RcExpoView.MIDDLE_TYPE_CURVE;
        public short curverParamk = 0;
        public boolean valiable = false;
        public baseConfig(){ };
        public baseConfig(int i ){ id = (short) i;};
        public boolean isValiable(){
            if( id < mRcCount && id >= 0 && valiable){
                return true;
            }else{
                return false;
            }
        }
    }
    public static class modeConfig{
        public short id;
        public String prefix;
        public String name;
    }
    private static  int mRcCount=0 ;

    mixConfig [] mixConfigs ;
    baseConfig [] baseConfigs ;
    public SharedPreferences prefs;
    Context mContent;
    private int mCurrentMode =0;

    public RcConfigParam(int rcCount,Context c){
        if( rcCount > 0 ){
            mRcCount = rcCount;
            mixConfigs = new mixConfig[mRcCount];
            baseConfigs = new baseConfig[mRcCount];
            for( int i=0; i< mRcCount; i++){
                baseConfigs[i]=new baseConfig();
                mixConfigs[i]=new mixConfig();
            }
        }
        mContent = c;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContent);
    }

    private void storeParamPrefs(String name, int val){
        //int r = prefs.getInt("num_runs", 0) + 1;
        prefs.edit().putInt(name, val).apply();
    }
    private int getParamPrefs(String name, int defval){
        return prefs.getInt(name, defval);
    }

    public void storeBaseConfig(baseConfig b){
        if( b.isValiable()){
            baseConfigs[b.id].id = b.id;
            baseConfigs[b.id].valiable = b.valiable;
            baseConfigs[b.id].maxValue = b.maxValue;
            baseConfigs[b.id].minValue = b.minValue;
            baseConfigs[b.id].revert = b.revert;
            baseConfigs[b.id].trim = b.trim;
            baseConfigs[b.id].curveType = b.curveType;
            baseConfigs[b.id].curverParamk = b.curverParamk;
        }
    }
    public baseConfig getBaseConfig(int id){
        if( id < mRcCount ){
            if( baseConfigs[id].valiable )
                return baseConfigs[id];
        }
        return null;
    }


    private void copyMixConfig(int id, mixConfig mc){
        mixConfigs[id].mainChan = mc.mainChan;
        mixConfigs[id].slaveChan = mc.slaveChan;
        mixConfigs[id].persenAtAdd = mc.persenAtAdd;
        mixConfigs[id].persenAtSub = mc.persenAtSub;
        mixConfigs[id].mainChanStartPoint = mc.mainChanStartPoint;
        mixConfigs[id].valiable = mc.valiable;

    }
    public void storeMixConfig(mixConfig b){
        for( int i =0 ; i< mRcCount; i++){
            if(mixConfigs[i].valiable ) {
                if (mixConfigs[i].mainChan == b.mainChan && mixConfigs[i].slaveChan == b.slaveChan){
                    copyMixConfig(i,b);
                    return;
                }
            }
        }
        //if on replace , put in new one
        for( int i =0 ; i< mRcCount; i++){
            if( !mixConfigs[i].valiable && b.mainChan >=0 && b.mainChan < mRcCount ){
                copyMixConfig(i,b);
            }
        }
    }
    public mixConfig getMixConfig(int mainChan, int slaveChan){
        for( int i =0 ; i< mRcCount; i++){
            if( mixConfigs[i]!=null && mixConfigs[i].valiable ) {
                if (mixConfigs[i].mainChan == mainChan && mixConfigs[i].slaveChan == slaveChan){
                    return mixConfigs[i];
                }
            }
        }
        return null;
    }
    public mixConfig getMixConfigBySlavchan(int slaveChan){
        for( int i =0 ; i< mRcCount; i++){
            if( mixConfigs[i]!=null && mixConfigs[i].valiable ) {
                if (mixConfigs[i].slaveChan == slaveChan){
                    return mixConfigs[i];
                }
            }
        }
        return null;
    }

}
