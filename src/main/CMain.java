package main;

import java.net.MalformedURLException;
import java.sql.SQLException;

//ds custom imports
import exceptions.CZEPEoIException;
import exceptions.CZEPGUIException;
import exceptions.CZEPMySQLManagerException;
import utility.CLogger;
import utility.CMySQLManager;
import learning.CLearnerBayes;
//import learning.CLearnerRandom;

public final class CMain
{
    //ds hardcoded values
    private static final String m_strConfigFileName = "config.txt";
    
    //ds default configuration parameters: gui 
    private static int m_iWindowWidth         = 1200;
    private static int m_iWindowHeight        = 800;
    
    //ds default configuration parameters: mysql
    private static String m_strMySQLDriver    = "com.mysql.jdbc.Driver";
    private static String m_strMySQLServerURL = "jdbc:mysql://pc-10129.ethz.ch:3306/domis";
    private static String m_strMySQLUsername  = "domis";
    private static String m_strMySQLPassword  = "N0effort";
    
    //ds MAIN
    public static void main( String[] args )
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) ZEP launched" );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) importing configuration from " + m_strConfigFileName + " .." );
        
        //ds start parsing the configuration file in fixed order
        //TODO
        
        System.out.println( "-------------------------------------------------------------------------------------------------" );
        System.out.println( "|                                         CONFIGURATION                                         |" );
        System.out.println( "-------------------------------------------------------------------------------------------------" );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) m_iWindowWidth=" + m_iWindowWidth );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) m_iWindowHeight=" + m_iWindowHeight );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) m_strMySQLUsername=" + m_strMySQLUsername );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) m_strMySQLPassword=" + m_strMySQLPassword );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) m_strMySQLDriver=" + m_strMySQLDriver );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) m_strMySQLServerURL=" + m_strMySQLServerURL );
        System.out.println( "-------------------------------------------------------------------------------------------------" );
        System.out.println( "|                                          LAUNCH PHASE                                         |" );
        System.out.println( "-------------------------------------------------------------------------------------------------" );
        
        //ds allocate the MySQL manager
        final CMySQLManager cMySQLManager = new CMySQLManager( m_strMySQLDriver, m_strMySQLServerURL, m_strMySQLUsername, m_strMySQLPassword );
        
        try
        {
            //ds launch the manager
            cMySQLManager.launch( );
        }
        catch( ClassNotFoundException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) ClassNotFoundException: " + e.getMessage( ) + " - could not establish MySQL connection" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) aborted" );
            return;
        }
        catch( SQLException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) SQLException: " + e.getMessage( ) + " - could not establish a valid MySQL connection" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) aborted" );
            return;            
        }
        catch( CZEPMySQLManagerException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) CZEPMySQLManagerException: " + e.getMessage( ) + " - failed to connect" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) aborted" );
            return;            
        }
        
        //ds allocate the learner
        final CLearnerBayes cLearner = new CLearnerBayes( cMySQLManager );
        
        try
        {
        	//ds launch the learner
        	cLearner.launch( );
        }
	    catch( SQLException e )
	    {
	        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) SQLException: " + e.getMessage( ) + " - could not connect to MySQL database" );
	        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) aborted" );
	        return;      
	    }
	    catch( CZEPMySQLManagerException e )
	    {
	        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) CZEPMySQLManagerException: " + e.getMessage( ) + " - could not launch learner" );
	        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) aborted" );
	        return;      
	    }
	    catch( CZEPEoIException e )
	    {
	        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) CZEPEoIException: " + e.getMessage( ) + " - no images in database" );
	        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) aborted" );
	        return;      
	    }
        
        //ds allocate a gui instance on the learner
        final CGUI cGUI = new CGUI( cLearner, cMySQLManager, m_iWindowWidth, m_iWindowHeight );

        try
        {
            //ds launch GUI (this will automatically fetch the first datapool since needed for display)
            cGUI.launch( );
        }
        catch( CZEPGUIException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) CZEPGUIException: " + e.getMessage( ) + " - could not initialize GUI" );
            cGUI.close( );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) aborted" );
            return;                  
        }
             
        System.out.println( "-------------------------------------------------------------------------------------------------" );
        System.out.println( "|                                      CLASSIFICATION PHASE                                      |" );
        System.out.println( "-------------------------------------------------------------------------------------------------" );
        
        //ds as long as the GUI is running
        while( cGUI.isActive( ) )
        {
            try
            {
                //ds avoid 100% cpu
                Thread.sleep( 100 );
                
                //ds check if we have to classify for the learner
                if( cLearner.isReadyToClassify( ) )
                {
                    //ds do classification
                    cLearner.classify( );
                }
            }
            catch( InterruptedException e )
            {
                _logMaster( cLearner, cMySQLManager, "<CMain>(main) InterruptedException: " + e.getMessage( ) );
                
                //ds not fatal
                System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) InterruptedException: " + e.getMessage( ) );
            }
            catch( MalformedURLException e )
            {
                _logMaster( cLearner, cMySQLManager, "<CMain>(main) MalformedURLException: " + e.getMessage( ) + " could not classify" );
                
                //ds fatal
                System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) MalformedURLException: " + e.getMessage( ) + " could not classify" );
                cGUI.close( );
                System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) aborted" );
                return; 
            }
            catch( SQLException e )
            {
                _logMaster( cLearner, cMySQLManager, "<CMain>(main) SQLException: " + e.getMessage( ) + " could not classify" );
                
                //ds fatal
                System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) SQLException: " + e.getMessage( ) + " could not classify" );
                cGUI.close( );
                System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) aborted" );
                return; 
            }
            catch( CZEPMySQLManagerException e )
            {
                _logMaster( cLearner, cMySQLManager, "<CMain>(main) CZEPMySQLManagerException: " + e.getMessage( ) + " could not classify" );
                
                //ds fatal
                System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) CZEPMySQLManagerException: " + e.getMessage( ) + " could not classify" );
                cGUI.close( );
                System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) aborted" );
                return; 
            }
        }
        
        //ds close GUI
        cGUI.close( );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) ZEP terminated" );
        return;
    }
    
    //ds MySQL logger
    private final static void _logMaster( final CLearnerBayes p_cLearner, final CMySQLManager p_cMySQLManager, final String p_strInfo )
    {
        //ds get username
        final String strUsername = p_cLearner.getUsername( );
        
        //ds if set
        if( null != strUsername )
        {
            try
            {
                //ds log
                p_cMySQLManager.logMaster( strUsername, p_strInfo );
            }
            catch( SQLException e )
            {
                System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(_logException) SQLException: " + e.getMessage( ) + " could not log to MySQL master" );
            }
        }
        else
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(_logException) could not log to master because of invalid username" );
        }
    }
}
