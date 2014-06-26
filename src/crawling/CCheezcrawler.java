package crawling;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Random;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.opencv.core.Core;

import exceptions.CZEPConfigurationException;
//ds custom
import exceptions.CZEPConversionException;
import exceptions.CZEPMySQLManagerException;
import utility.CConverter;
import utility.CDataPoint;
import utility.CImageHandler;
import utility.CImporter;
import utility.CLogger;
import utility.CMySQLManager;
import utility.CTag;

public class CCheezcrawler extends Thread
{
    //ds crawling attributes set by configuration file
    final private URL m_cMasterURL;
    final private int m_iTimeoutMS;
    final private int m_iLogLevel;
    
    //ds internal attributes
    private int m_iCurrentNumberOfImages        = 0;
    private int m_iCurrentNumberOfPages         = 0;
    private int m_iCurrentNumberOfFeatures      = 0;
    private int m_iCurrentNumberOfImagesToMySQL = 0;
    private int m_iCurrentNumberOfAlreadyMySQL  = 0;
    private int m_iNumberOfExceptions           = 0;
    
    //ds total datapoints to acquire
    final private int m_iTargetNumberOfImages;
    final private int m_iMaximumPageNumber;
    
    //ds simulate human behavior
    final private static Random m_cGenerator = new Random( );
    
    //ds query for like/dislikes
    final String m_strQueryRating = "http://app.cheezburger.com/Rating/Scores?callback=jQuery190005670550150134579_1397320129116&section=3&assetIds=";
    
    //ds mysql manager
    private final CMySQLManager m_cMySQLManager;
    
    //ds constructor
    public CCheezcrawler( final CMySQLManager p_cMySQLManager, final URL p_cMasterURL, final int p_iTargetNumberOfImages, final int p_iMaximumPageNumber, final int p_iTimeoutMS, final int p_iLogLevel )
    {
        //ds assign values
        m_cMySQLManager         = p_cMySQLManager;
        m_cMasterURL            = p_cMasterURL;
        m_iTargetNumberOfImages = p_iTargetNumberOfImages;
        m_iMaximumPageNumber    = p_iMaximumPageNumber;
        m_iTimeoutMS            = p_iTimeoutMS;
        m_iLogLevel             = p_iLogLevel;
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(CCheezcrawler) instance allocated" );
    };
    
    //ds standalone
    public static void main( String[] args ) throws IOException, CZEPConversionException
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) MOCK LAUNCHED" );
        
        //ds load OpenCV core
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
        
        //BufferedImage imgPDDrawing    = ImageIO.read( new File( "photo_detection_drawing_image.jpg" ) );
        //BufferedImage imgPDPhotograph = ImageIO.read( new File( "photo_detection_photograph_image.jpg" ) );
        //BufferedImage imgTextDetectionEasy = ImageIO.read( new File( "advice_mallard.jpg" ) );
        BufferedImage imgTextDetectionHard = ImageIO.read( new File( "test.jpeg" ) );
        
        //CImageHandler.isAPhotographRGB( imgPDDrawing );
        //CImageHandler.isAPhotographRGB( imgPDPhotograph );
        //System.out.println( "text percentage: " + CImageHandler.getTextPercentageCanny( imgTextDetectionEasy ) );
        System.out.println( "text percentage: " + CImageHandler.getTextPercentageCanny( imgTextDetectionHard ) );
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) MOCK TERMINATED" );
        return;
        
        
        /*ds default configuration parameters: mysql
        String strMySQLServerURL = "";
        String strMySQLUsername  = "";
        String strMySQLPassword  = "";
        
        try
        {
            //ds MySQL
            strMySQLServerURL = CImporter.getAttribute( "config.txt", "m_strMySQLServerURL" );
            strMySQLUsername  = CImporter.getAttribute( "config.txt", "m_strMySQLUsername" );
            strMySQLPassword  = CImporter.getAttribute( "config.txt", "m_strMySQLPassword" );
        }
        catch( CZEPConfigurationException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) CZEPConfigurationException: " + e.getMessage( ) + " - error during config file parsing" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) aborted" );
            return;            
        }
        
        //ds default configuration parameters: cheezcrawler
        final URL cMasterURL_Cheezcrawler      = new URL( "http://memebase.cheezburger.com/" );
        final int iNumberOfImages_Cheezcrawler = 10000;
        final int iMaximumPage_Cheezcrawler    = 4332;
        final int iTimeoutMS_Cheezcrawler      = 60000;
        final int iLogLevel_Cheezcrawler       = 1;
        
    	//ds load OpenCV core
    	System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
        
        //ds allocate the MySQL manager
        final CMySQLManager cMySQLManager = new CMySQLManager( strMySQLServerURL, strMySQLUsername, strMySQLPassword );
        
        try
        {
            //ds launch the manager
            cMySQLManager.launch( );
        }
        catch( ClassNotFoundException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(main) ClassNotFoundException: " + e.getMessage( ) + " - could not establish MySQL connection" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(main) aborted" );
            return;
        }
        catch( SQLException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(main) SQLException: " + e.getMessage( ) + " - could not establish a valid MySQL connection" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(main) aborted" );
            return;            
        }
        catch( CZEPMySQLManagerException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(main) CZEPMySQLManagerException: " + e.getMessage( ) + " - failed to connect" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(main) aborted" );
            return;            
        }

        //ds allocate a crawler instance
        final Thread cCrawler = new Thread( new CCheezcrawler( cMySQLManager, cMasterURL_Cheezcrawler, iNumberOfImages_Cheezcrawler, iMaximumPage_Cheezcrawler, iTimeoutMS_Cheezcrawler, iLogLevel_Cheezcrawler ) );
        
        //ds start it (blocking)
        cCrawler.run( );*/
    }

    //ds starts crawling
    public void run( )
    {
        try
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) target number of datapoints: " + m_iTargetNumberOfImages );
            
            //ds crawl frontpage
            _crawlPage( m_cMasterURL );
            
            //ds current page
            int iCurrentPage = 2;
            
            //ds start infinite loop until target is reached
            while( !Thread.interrupted( ) && ( m_iTargetNumberOfImages > m_iCurrentNumberOfImagesToMySQL ) && ( m_iMaximumPageNumber > iCurrentPage ) )
            {
                try
                {
                    //ds simply add the page number
                    _crawlPage( new URL( m_cMasterURL.toString( ) + "page/" + Integer.toString( iCurrentPage ) ) );
                }
                catch( Exception e )
                {
                    //ds not fatal
                    System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) Exception: " + e.getMessage( ) + " - skipping page: " + iCurrentPage );
                    ++m_iNumberOfExceptions;
                    
                    //ds take a break
                    sleep( 60000+m_cGenerator.nextInt( 500 ) );
                }
                
                //ds log all 10 pages
                if( 0 == iCurrentPage%10 )
                {
                    //ds info
                    System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) crawled pages: " + iCurrentPage + " added points: " + m_iCurrentNumberOfImagesToMySQL + " already: " + m_iCurrentNumberOfAlreadyMySQL );
                }
                
                //ds increase counter
                ++iCurrentPage;
            }
            
            //ds we got interrupted
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) crawling terminated by main" );
            _printDownloadSummary( );
            return;
        }
        catch( SQLException e )
        {
            
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) SQLException: " + e.getMessage( ) + " - could not store last datapoint in MySQL (" + m_iCurrentNumberOfImages + ")" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) aborted" );
            return;
        }
        catch( MalformedURLException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) MalformedURLException: " + e.getMessage( ) + " - crawler failure" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) aborted" );
            return;
        }
        catch( IOException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) IOException: " + e.getMessage( ) + " - crawler failure" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) aborted" );
            return;
        }
        catch( CZEPConversionException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) CZEPConversionException: " + e.getMessage( ) + " - internal image conversion error" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) aborted" );
            return;
        }
        catch( InterruptedException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) InterruptedException: " + e.getMessage( ) + " - crawling terminated by signal" );
            _printDownloadSummary( );
            return;
        }
    }
    
    private void _crawlPage( final URL p_cURL ) throws IOException, InterruptedException, SQLException, CZEPConversionException
    {        
        if( 0 == m_iLogLevel )
        {
            System.out.println( "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(_crawlPage) current NoI: " + m_iCurrentNumberOfImages );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(_crawlPage) current NoP: " + m_iCurrentNumberOfPages );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(_crawlPage)   accessing: " + p_cURL );
            System.out.println( "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" );
        }

        //ds parse the HTML tree from the current page
        final Document cCurrentPage = Jsoup.connect( p_cURL.toString( ) ).timeout( m_iTimeoutMS ).get( );

        //ds get all posts on this page
        final Elements vec_cPosts = cCurrentPage.getElementsByClass( "post" );
        
        //ds loop thru all posts
        for( Element cCurrentPost : vec_cPosts )
        {   
            //ds get the image holder element (only one per post)
            final Element cImageHolder = cCurrentPost.getElementsByClass( "event-item-lol-image" ).first( );
            
            //ds if something was found
            if( null != cImageHolder )
            {
                //ds obtain relevant attributes
                final String strTitle   = cImageHolder.attr( "title" );
                final URL cDownloadPath = new URL( cImageHolder.attr( "src" ) );
                
                //ds get file extension
                final String strExtension = CConverter.getFileExtensionFromURL( cDownloadPath ).toLowerCase( );
                
                //ds only accept jpeg/jpg/jpe/gif
                if( strExtension.matches( "jpeg" ) || strExtension.matches( "jpg" ) || strExtension.matches( "jpe" ) || strExtension.matches( "gif" ) )
                {
                    //ds get post number (layout: post-1234..
                    final String strPostNumber = cCurrentPost.id( ).substring( 5 );
                    
                    //ds execute the request
                    final String strResponse = Jsoup.connect( m_strQueryRating + strPostNumber ).ignoreContentType( true ).timeout( m_iTimeoutMS ).get( ).text( );
                    
                    //ds find Wins and Fails - custom parsing
                    final String strKeyWordWins  = "Wins";
                    final String strKeyWordFails = "Fails";
                    final int iPositionWinsStart  = strResponse.indexOf( strKeyWordWins );
                    final int iPositionWinsEnd    = strResponse.indexOf( ',', iPositionWinsStart );
                    final int iPositionFailsStart = strResponse.indexOf( strKeyWordFails );
                    final int iPositionFailsEnd   = strResponse.indexOf( '}', iPositionFailsStart );
                    
                    //ds set number strings (+2 because of the ": characters)
                    final String strLikes    = strResponse.substring( iPositionWinsStart+strKeyWordWins.length( )+2, iPositionWinsEnd );
                    final String strDislikes = strResponse.substring( iPositionFailsStart+strKeyWordFails.length( )+2, iPositionFailsEnd );

                	//ds get comment count
                	final String strCountComments = ( cCurrentPost.getElementsByClass( "js-comment-count" ).first( ) ).text( );
                	
                	//ds default values
                	int iLikes    	   = 0;
                	int iDislikes      = 0;
                	int iCountComments = 0;
                	
                	try
                	{
                        //ds try to extract the integer values
                	    iLikes         = Integer.valueOf( strLikes );
                	    iDislikes      = Integer.valueOf( strDislikes );
                	    iCountComments = Integer.valueOf( strCountComments );
                	}
                	catch( NumberFormatException e )
                	{
                	    //ds not fatal
                        System.out.println( "[" + CLogger.getStamp( ) + "]<CMainCheezcrawler>(_crawlPage) NumberFormatException: " + e.getMessage( ) + " parsing error - defaulted to value: 0" );                	    
                	}
                	
                	//ds get the image
                	final BufferedImage cImage = ImageIO.read( cDownloadPath );
                	
                	//ds OpenCV operations - defaulted for gifs
                	double dTextAmount = 0.0;   if( !strExtension.matches( "gif" ) ){ dTextAmount = CImageHandler.getTextPercentageCanny( cImage ); }
                	boolean bIsPhoto   = false; if( !strExtension.matches( "gif" ) ){ bIsPhoto    = CImageHandler.isAPhotographGray( cImage, 0.1 ); }
                	
	                //ds get the tag holder (only one per post)
	                final Element cTagHolder = cCurrentPost.getElementsByClass( "tags" ).first( );
	                
	                //ds if something was found
	                if( null != cTagHolder )
	                {
	                    //ds get the tag vector
	                    final Elements vec_cTags = cTagHolder.getElementsByClass( "alt" );
	                    
	                    //ds tag vector
	                    Vector< CTag > vec_Tags = new Vector< CTag >( );
	                    
	                    //ds loop over all tags
	                    for( Element cTag: vec_cTags )
	                    {
	                        //ds add the current tag to the vector
	                        vec_Tags.add( new CTag( 0, cTag.text( ).toLowerCase( ), 0 ) );
	                    }
	                    
	                    //ds create the datapoint
	                    final CDataPoint cDataPoint = new CDataPoint( 0, cDownloadPath, strTitle, strExtension, iLikes, iDislikes, iCountComments, vec_Tags.size( ), bIsPhoto, dTextAmount , vec_Tags );
	                    
	                    //ds increase image counter
	                    ++m_iCurrentNumberOfImages;
	                    
	                    //ds and feature counter
	                    m_iCurrentNumberOfFeatures += vec_Tags.size( );
	                    
	                    try
	                    {
	                        //ds insert
	                        m_cMySQLManager.insertDataPoint( cDataPoint );
	                        
	                        //ds increase if insertion was successful
	                        ++m_iCurrentNumberOfImagesToMySQL;
	                        
	                        //ds info
	                        if( 1 >= m_iLogLevel )
	                        {
	                            System.out.println( "[" + CLogger.getStamp( ) + "]<CMainCheezcrawler>(_crawlPage) added new image (" + m_iCurrentNumberOfImages + ") page: " + p_cURL.toString( ) );
	                        }
	                        
	                        //ds detailed info
	                        if( 0 == m_iLogLevel )
	                        {
	                            System.out.println( "[" + CLogger.getStamp( ) + "]<CMainCheezcrawler>(_crawlPage)        Title: " + cDataPoint.getTitle( ) );
	                            System.out.println( "[" + CLogger.getStamp( ) + "]<CMainCheezcrawler>(_crawlPage) Download URL: " + cDataPoint.getURL( ) );
	                            System.out.println( "[" + CLogger.getStamp( ) + "]<CMainCheezcrawler>(_crawlPage)  Text Amount: " + cDataPoint.getTextAmount( ) );
	                            System.out.println( "[" + CLogger.getStamp( ) + "]<CMainCheezcrawler>(_crawlPage)      IsPhoto: " + cDataPoint.isPhoto( ) );
	                            System.out.println( "[" + CLogger.getStamp( ) + "]<CMainCheezcrawler>(_crawlPage)         Type: " + strExtension );
	                            System.out.println( "[" + CLogger.getStamp( ) + "]<CMainCheezcrawler>(_crawlPage)        Likes: " + cDataPoint.getLikes( ) );
	                            System.out.println( "[" + CLogger.getStamp( ) + "]<CMainCheezcrawler>(_crawlPage)     Dislikes: " + cDataPoint.getDislikes( ) );
	                            System.out.println( "[" + CLogger.getStamp( ) + "]<CMainCheezcrawler>(_crawlPage)     Comments: " + cDataPoint.getCountComments( ) );
	                            System.out.println( "[" + CLogger.getStamp( ) + "]<CMainCheezcrawler>(_crawlPage)         Tags: " + cDataPoint.getTags( ) );
	                        }
	                    }
	                    catch( CZEPMySQLManagerException e )
	                    {
	                        if( 1 >= m_iLogLevel )
	                        {
	                            //ds not fatal since we have the data backed up in our map
	                            System.out.println( "[" + CLogger.getStamp( ) + "]<CMainCheezcrawler>(_crawlPage) CZEPMySQLManagerException: " + e.getMessage( ) + " - datapoint already in database (" + m_iCurrentNumberOfImages + ")" );
	                        }
	                        
	                        ++m_iCurrentNumberOfAlreadyMySQL;
	                    }
                	}
                }
            }
        }
        
        //ds give the page a break in a humanly manner (sleep between 0.5 and 1s)
        sleep( 500+m_cGenerator.nextInt( 500 ) );
        
        //ds done, increase page counter
        m_iCurrentNumberOfPages++;       
    }
    
    private void _printDownloadSummary( )
    {
        System.out.println( "-------------------------------------------------------------------------------------------------" );
        System.out.println( "|                                    SUMMARY: CHEEZCRAWLER                                      |" );
        System.out.println( "-------------------------------------------------------------------------------------------------" );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run)     total pages crawled: " + ( m_iCurrentNumberOfPages+1 ) );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run)  total images retrieved: " + m_iCurrentNumberOfImages );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run)   images added to MySQL: " + m_iCurrentNumberOfImagesToMySQL );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) images already in MySQL: " + m_iCurrentNumberOfAlreadyMySQL );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run)          total features: " + m_iCurrentNumberOfFeatures );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run)     features per sample: " + String.format( "%.3g", ( double )m_iCurrentNumberOfFeatures/m_iCurrentNumberOfImages ) );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run)        total exceptions: " + m_iNumberOfExceptions );
        System.out.println( "-------------------------------------------------------------------------------------------------" );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) terminated" );        
    }
}
