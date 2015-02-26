package com.example.testimpedance;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class AudioDeviceStereo {
	AudioTrack track;
	double amp_offset;
	   byte[] buffer = new byte[2048];
	
	
	   public AudioDeviceStereo( )
	   {
	      int minSize =AudioTrack.getMinBufferSize( 44100, AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT );        
	      track = new AudioTrack( AudioManager.STREAM_MUSIC, 44100, 
	                                        AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT, 
	                                        minSize, AudioTrack.MODE_STREAM);
	      track.play();        
	   }	   
	   
	   public void writeSamples(float[] samples) 
	   {	
	      fillBuffer( samples );
	      track.write(buffer, 0, buffer.length );
	   }
	   public void changeTheOffset(double offset){
		   amp_offset = offset;
	   }
	   private void fillBuffer( float[] samples )
	   {
	      if( buffer.length < samples.length ){
	         buffer = new byte[samples.length];
	      }
	      for(int i = 0, j=0; j< samples.length;i+=4){
    	  buffer[i] = (byte)(samples[j]*(Byte.MAX_VALUE-amp_offset));
    	  buffer[i+1] = (byte)(samples[j+1]*(Byte.MAX_VALUE-amp_offset));
    	  buffer[i+2]= (byte)(samples[j]*(Byte.MIN_VALUE+amp_offset));
    	  buffer[i+3]= (byte)(samples[j+1]*(Byte.MIN_VALUE+amp_offset));
    	  j+=2;
	    	  
      }
	 
	   }
	   public void release(){
		   track.pause();
		   track.flush();
		   track.stop();
		   track.release();
		   track = null;
	   }
}
