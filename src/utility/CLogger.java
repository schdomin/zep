package utility;

import java.text.SimpleDateFormat;

public final class CLogger
{
    //ds time stamp wrapper
    public static String getStamp( )
    {
        //ds get a string format element
        final SimpleDateFormat cCurrentStamp = new SimpleDateFormat( "HH:mm:ss.SSS" );
        
        //ds return the formatted time stamp
        return cCurrentStamp.format( System.currentTimeMillis( ) );
    }
}
