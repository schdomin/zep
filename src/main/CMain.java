package main;

import java.sql.SQLException;

//ds custom imports
import exceptions.CZEPConfigurationException;
import exceptions.CZEPEoIException;
import exceptions.CZEPGUIException;
import exceptions.CZEPMySQLManagerException;
import utility.CImporter;
import utility.CLogger;
import utility.CMySQLManager;
import learning.CLearnerBayes;
//import learning.CLearnerRandom;

public abstract class CMain
{
    //ds hardcoded values
    private static final String m_strConfigFileName = "config.txt";
    
    //ds default configuration parameters: gui 
    private static int m_iWindowWidth  = -1;
    private static int m_iWindowHeight = -1;
    
    //ds default configuration parameters: mysql
    private static String m_strMySQLServerURL = "";
    private static String m_strMySQLUsername  = "";
    private static String m_strMySQLPassword  = "";
    
    //ds MAIN
    public static void main( String[] args )
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) ZEP launched" );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) importing configuration from [" + m_strConfigFileName + "]" );
        
        try
        {
            //ds GUI
            m_iWindowWidth  = Integer.valueOf( CImporter.getAttribute( m_strConfigFileName, "m_iWindowWidth" ) );
            m_iWindowHeight = Integer.valueOf( CImporter.getAttribute( m_strConfigFileName, "m_iWindowHeight" ) );
            
            //ds MySQL
            m_strMySQLServerURL = CImporter.getAttribute( m_strConfigFileName, "m_strMySQLServerURL" );
            m_strMySQLUsername  = CImporter.getAttribute( m_strConfigFileName, "m_strMySQLUsername" );
            m_strMySQLPassword  = CImporter.getAttribute( m_strConfigFileName, "m_strMySQLPassword" );
        }
        catch( CZEPConfigurationException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) CZEPConfigurationException: " + e.getMessage( ) + " - error during config file parsing" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) aborted" );
            return;            
        }
        catch( NumberFormatException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) NumberFormatException: " + e.getMessage( ) + " - could not convert attribute values to numerical" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) aborted" );
            return;            
        }
        
        System.out.println( "-------------------------------------------------------------------------------------------------" );
        System.out.println( "|                                         CONFIGURATION                                         |" );
        System.out.println( "-------------------------------------------------------------------------------------------------" );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) m_iWindowWidth=" + m_iWindowWidth );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) m_iWindowHeight=" + m_iWindowHeight );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) m_strMySQLUsername=" + m_strMySQLUsername );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) m_strMySQLServerURL=" + m_strMySQLServerURL );
        System.out.println( "-------------------------------------------------------------------------------------------------" );
        System.out.println( "|                                          LAUNCH PHASE                                         |" );
        System.out.println( "-------------------------------------------------------------------------------------------------" );
        
        //ds allocate the MySQL manager
        final CMySQLManager cMySQLManager = new CMySQLManager( m_strMySQLServerURL, m_strMySQLUsername, m_strMySQLPassword );
        
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
	        System.exit( 0 );
	        return;      
	    }
	    catch( CZEPMySQLManagerException e )
	    {
	        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) CZEPMySQLManagerException: " + e.getMessage( ) + " - could not launch learner" );
	        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) aborted" );
	        System.exit( 0 );
	        return;      
	    }
	    catch( CZEPEoIException e )
	    {
	        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) CZEPEoIException: " + e.getMessage( ) + " - no images in database" );
	        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) aborted" );
	        System.exit( 0 );
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
            System.exit( 0 );
            return;                  
        }
        catch( SQLException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) SQLException: " + e.getMessage( ) + " - could not initialize GUI" );
            cGUI.close( );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) aborted" );
            System.exit( 0 );
            return;                  
        }
        catch( CZEPMySQLManagerException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) CZEPMySQLManagerException: " + e.getMessage( ) + " - could not connect to MySQL" );
            cGUI.close( );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) aborted" );
            System.exit( 0 );
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
            catch( CZEPEoIException e )
            {
                _logMaster( cLearner, cMySQLManager, "<CMain>(main) CZEPEoIException: " + e.getMessage( ) );
                
                //ds fatal
                System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) CZEPEoIException: " + e.getMessage( ) );
                cGUI.close( );
                System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) aborted" );
                System.exit( 0 );
                return; 
            }
        }
        
        cGUI.close( );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(main) ZEP terminated" );
        System.exit( 0 );
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
                System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(_logMaster) SQLException: " + e.getMessage( ) + " could not log to MySQL master" );
            }
        }
        else
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMain>(_logMaster) could not log to master because of empty username" );
        }
    }
}
