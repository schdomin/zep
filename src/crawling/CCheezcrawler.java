package crawling;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Random;
import java.util.Vector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.opencv.core.Core;

//ds custom
import exceptions.CZEPMySQLManagerException;
import utility.CConverter;
import utility.CDataPoint;
import utility.CLogger;
import utility.CMySQLManager;

public class CCheezcrawler extends Thread
{
    //ds crawling attributes set by configuration file
    final private URL m_cMasterURL;
    final private int m_iNumberOfPages;
    final private int m_iTimeoutMS;
    final private int m_iLogLevel;
    
    //ds internal attributes
    private int m_iCurrentNumberOfImages        = 0;
    private int m_iCurrentNumberOfPages         = 0;
    private int m_iCurrentNumberOfFeatures      = 0;
    private int m_iCurrentNumberOfImagesToMySQL = 0;
    private int m_iCurrentNumberOfAlreadyMySQL  = 0;
    
    //ds simulate human behavior
    final private static Random m_cGenerator = new Random( );
    
    //ds mysql manager
    private final CMySQLManager m_cMySQLManager;
    
    //ds constructor
    public CCheezcrawler( final CMySQLManager p_cMySQLManager, final URL p_cMasterURL, final int p_iNumberOfPages, final int p_iTimeoutMS, final int p_iLogLevel )
    {
        //ds assign values
        m_cMySQLManager  = p_cMySQLManager;
        m_cMasterURL     = p_cMasterURL;
        m_iNumberOfPages = p_iNumberOfPages;
        m_iTimeoutMS     = p_iTimeoutMS;
        m_iLogLevel      = p_iLogLevel;
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(CCheezcrawler) instance allocated" );
    };
    
    //ds standalone
    public static void main( String[] args ) throws MalformedURLException
    {
        //ds default configuration parameters: mysql
        final String strMySQLDriver    = "com.mysql.jdbc.Driver";
        final String strMySQLServerURL = "jdbc:mysql://pc-10129.ethz.ch:3306/domis";
        final String strMySQLUsername  = "domis";
        final String strMySQLPassword  = "N0effort";
        
        //ds default configuration parameters: cheezcrawler
        final URL cMasterURL_Cheezcrawler      = new URL( "http://memebase.cheezburger.com/" );
        final int iNumberOfPages_Cheezcrawler  = 0;
        final int iTimeoutMS_Cheezcrawler      = 10000;
        final int iLogLevel_Cheezcrawler       = 0;
        
    	//ds load OpenCV core
    	System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
        
        //ds allocate the MySQL manager
        final CMySQLManager cMySQLManager = new CMySQLManager( strMySQLDriver, strMySQLServerURL, strMySQLUsername, strMySQLPassword );
        
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
        final Thread cCrawler = new Thread( new CCheezcrawler( cMySQLManager, cMasterURL_Cheezcrawler, iNumberOfPages_Cheezcrawler, iTimeoutMS_Cheezcrawler, iLogLevel_Cheezcrawler ) );
        
        //ds start it (blocking)
        cCrawler.run( );
    }

    //ds starts crawling
    public void run( )
    {
        try
        {
            //ds crawl frontpage
            _crawlPage( m_cMasterURL );
            
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) crawled first page: " + m_cMasterURL );
            
            //ds if we have a finite number of pages to crawl
            if( 0 != m_iNumberOfPages )
            {
                //ds crawl additional pages
                for( int i = 2; i < m_iNumberOfPages; ++i )
                {
                    //ds before crawling on check master
                    if( Thread.interrupted( ) )
                    {
                        System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) crawling terminated by main" );
                        _printDownloadSummary( );
                        return;
                    }
                    
                    //ds simply add the page number
                    _crawlPage( new URL( m_cMasterURL.toString( ) + "page/" + Integer.toString( i ) ) );
                }
            }
            else
            {
                System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) starting infinite crawling" );
                
                //ds current page
                int iCurrentPage = 2;
                
                //ds start infinite loop
                while( !Thread.interrupted( ) )
                {
                    //ds simply add the page number
                    _crawlPage( new URL( m_cMasterURL.toString( ) + "page/" + Integer.toString( iCurrentPage ) ) );
                    
                    //ds increase counter
                    ++iCurrentPage;
                }
                
                //ds we got interrupted
                System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) crawling terminated by main" );
                _printDownloadSummary( );
                return;
            }
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
        catch( InterruptedException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) InterruptedException: " + e.getMessage( ) + " - crawling terminated by signal" );
            _printDownloadSummary( );
            return;
        }
       
        System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) download complete" );
        _printDownloadSummary( );
        return;
    }
    
    private void _crawlPage( final URL p_cURL ) throws IOException, InterruptedException, SQLException
    {        
        if( 0 == m_iLogLevel )
        {
            System.out.println( "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(_crawlPage) current NoI: " + m_iCurrentNumberOfImages );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(_crawlPage) current NoP: " + m_iCurrentNumberOfPages );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(_crawlPage)   accessing: " + p_cURL );
            System.out.println( "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" );
        }
        
        //ds allocate a webpage handle
        final Document cCurrentPage;
        
        try
        {
            //ds try to open the current page
            cCurrentPage = Jsoup.connect( p_cURL.toString( ) ).timeout( m_iTimeoutMS ).get( );
        }
        catch( IOException e )
        {
            //ds in case of error return for this page
            System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(_crawlPage) IOException: " + e.getMessage( ) + " - skipped page: " + p_cURL );
            return;
        }
        
        //ds get all posts on this page
        final Elements vec_cPosts = cCurrentPage.getElementsByClass( "content-card" );
        
        //ds loop thru all posts
        for( Element cCurrentPost : vec_cPosts )
        {   
            //ds get the image holder element (only one per post)
            final Element cImageHolder = cCurrentPost.getElementsByClass( "event-item-lol-image" ).first( );
            
            //ds if something was found
            if( null != cImageHolder )
            {
                //ds obtain relevant attributes
                final String strTitle        = cImageHolder.attr( "title" );
                final String strDownloadPath = cImageHolder.attr( "src" );
                
                //ds check file extension
                final String strExtension = CConverter.getFileExtensionFromURL( new URL( strDownloadPath ) ).toLowerCase( );

                //ds get the tag holder (only one per post)
                final Element cTagHolder = cCurrentPost.getElementsByClass( "tags" ).first( );
                
                //ds if something was found
                if( null != cTagHolder )
                {
                    //ds get the tag vector
                    final Elements vec_cTags = cTagHolder.getElementsByClass( "alt" );
                    
                    //ds tag vector
                    Vector< String > vec_strTags = new Vector<String>( );
                    
                    //ds loop over all tags
                    for( Element cTag: vec_cTags )
                    {
                        //ds add the current tag to the vector
                        vec_strTags.add( cTag.text( ).toLowerCase( ) );
                    }
                    
                    //ds create the datapoint
                    final CDataPoint cDataPoint = new CDataPoint( 0, new URL( strDownloadPath ), strTitle, strExtension, vec_strTags );
                    
                    //ds check if not a gif
                    if( "gif" != strExtension )
                    {
	                    //ds increase image counter
	                    ++m_iCurrentNumberOfImages;
	                    
	                    //ds and feature counter
	                    m_iCurrentNumberOfFeatures += vec_strTags.size( );
	                    
	                    try
	                    {
	                        //ds insert
	                        m_cMySQLManager.insertDataPoint( cDataPoint );
	                        
	                        //ds increase if insertion was successful
	                        ++m_iCurrentNumberOfImagesToMySQL;
	                        
	                        //ds info
	                        if( 1 >= m_iLogLevel )
	                        {
	                            System.out.println( "[" + CLogger.getStamp( ) + "]<CMainCheezcrawler>(_crawlPage) added new image (" + m_iCurrentNumberOfImages + ")" );
	                        }
	                        
	                        //ds detailed info
	                        if( 0 == m_iLogLevel )
	                        {
	                            System.out.println( "[" + CLogger.getStamp( ) + "]<CMainCheezcrawler>(_crawlPage)           title: " + cDataPoint.getTitle( ) );
	                            System.out.println( "[" + CLogger.getStamp( ) + "]<CMainCheezcrawler>(_crawlPage)    download URL: " + cDataPoint.getURL( ) );
	                            System.out.println( "[" + CLogger.getStamp( ) + "]<CMainCheezcrawler>(_crawlPage)            type: " + cDataPoint.getType( ) );
	                            System.out.println( "[" + CLogger.getStamp( ) + "]<CMainCheezcrawler>(_crawlPage)            tags: " + cDataPoint.getTags( ) );
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
        System.out.println( "-------------------------------------------------------------------------------------------------" );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CCheezcrawler>(run) terminated" );        
    }
}
