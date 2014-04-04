package utility;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

//ds custom imports
import exceptions.CZEPConversionException;

public final class CConverter
{
    //ds String to Vector converter
    public static Vector< String > parseVector( final String p_strVectorString ) throws CZEPConversionException
    {
        //ds get string length
        final int iLength = p_strVectorString.length( );
        
        //ds return if empty
        if( 0 == iLength )
        {
            //ds escape
            throw new CZEPConversionException( "received empty string" );
        }
        
        //ds first check if valid format [abc,defgh,ijklmnopq,rs]
        if( ( char )91 != p_strVectorString.charAt( 0 ) || ( char )93 != p_strVectorString.charAt( iLength-1 ) )
        {
            //ds escape
            throw new CZEPConversionException( "invalid string format" );
        }
        
        //ds initialize new vector
        Vector< String > vecParsed = new Vector< String >( );
        
        //ds current element position
        int iCurrentStart = 1;
        int iCurrentEnd   = 1;
        
        //ds while we didnt reach the end
        while( -1 != iCurrentEnd )
        {
            //ds find the next comma
            iCurrentEnd = p_strVectorString.indexOf( ( char )44, iCurrentStart );
            
            //ds check if its not the last element
            if( -1 != iCurrentEnd )
            {
                //ds extract normally and add it to the vector
                vecParsed.add( p_strVectorString.substring( iCurrentStart, iCurrentEnd ) );
                
                //ds update start to end for the next round (+2 to skip the comma and the space after an element)
                iCurrentStart = iCurrentEnd+2;
            }
            else
            {
                //ds add the rest
                vecParsed.add(  p_strVectorString.substring( iCurrentStart, iLength-1 ) );
            }
        }
        
        //ds check if nothing has been added
        if( vecParsed.isEmpty( ) )
        {
            //ds escape
            throw new CZEPConversionException( "could not parse any elements" );            
        }
        
        return vecParsed;
    }
    
    //ds determine mime type
    public final static String getFileExtensionFromURL( final URL p_cURL ) throws IOException
    {
        //ds get an input stream on the current url
        final ImageInputStream cIIS = ImageIO.createImageInputStream( p_cURL.openStream( ) );
        
        //ds attach an image reader to it
        final Iterator< ImageReader > cImageReader = ImageIO.getImageReaders(cIIS);

        //ds if there is a next field
        if( cImageReader.hasNext( ) )
        {
            //ds iterate over the file
            final ImageReader cImageReaderCurrent = cImageReader.next( );
            
            //ds and return
            return cImageReaderCurrent.getFormatName( );
        }
        else
        {
            throw new IOException( "received invalid URL" );
        }
    }
}
