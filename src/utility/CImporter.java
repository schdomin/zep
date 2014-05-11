package utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import exceptions.CZEPConfigurationException;

public final class CImporter
{
    //ds parse string from config file
    public static String getAttribute( final String p_strConfigFilePath, final String p_strAttributeName ) throws CZEPConfigurationException
    {
        try
        {
            //ds get a sophisticated reader object
            Scanner cScanner = new Scanner( new File( p_strConfigFilePath ) );

            //ds start reading
            while( cScanner.hasNext( ) )
            {
                //ds get current line
                final String strLine = cScanner.nextLine( );
                
                //ds check if we have our attribute in there
                if( strLine.contains( p_strAttributeName ) )
                {
                    //ds get the position of the equal sign
                    final int iIndex = strLine.indexOf( '=' );
                    
                    //ds if set
                    if( -1 != iIndex )
                    {
                        //ds close scanner
                        cScanner.close( );
                        
                        //ds parse the value after the sign and return
                        return strLine.substring( iIndex+1 );
                    }
                    else
                    {
                        //ds close scanner
                        cScanner.close( );
                        
                        //ds syntax error in config file
                        throw new CZEPConfigurationException( "could not find '=' sign" );
                    }
                    
                }
            }
            
            //ds nothing found - close scanner
            cScanner.close( );
            
            //ds escape
            throw new CZEPConfigurationException( "could not find attribute: [" + p_strAttributeName + "]" );
        }
        catch( FileNotFoundException e )
        {
            //ds escape
            throw new CZEPConfigurationException( "FileNotFoundException e: " + e.getMessage( ) );
        }
    }
}
