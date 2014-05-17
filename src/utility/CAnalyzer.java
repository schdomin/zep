package utility;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import exceptions.CZEPConfigurationException;
import exceptions.CZEPMySQLManagerException;

public abstract class CAnalyzer
{
    //ds mysql manager
    private static CMySQLManager m_cMySQLManager = null;
    
    //ds standalone
    public static void main( String[] args )
    {
      //ds default configuration parameters: mysql
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
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(main) CZEPConfigurationException: " + e.getMessage( ) + " - error during config file parsing" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(main) aborted" );
            return;            
        }
        
        //ds allocate a MySQL manager instance
        m_cMySQLManager = new CMySQLManager( strMySQLServerURL, strMySQLUsername, strMySQLPassword );
        
        try
        {
            //ds launch the manager
            m_cMySQLManager.launch( );
        }
        catch( ClassNotFoundException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CAnalyzer>(main) ClassNotFoundException: " + e.getMessage( ) + " - could not establish MySQL connection" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CAnalyzer>(main) aborted" );
            return;
        }
        catch( SQLException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CAnalyzer>(main) SQLException: " + e.getMessage( ) + " - could not establish a valid MySQL connection" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CAnalyzer>(main) aborted" );
            return;            
        }
        catch( CZEPMySQLManagerException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CAnalyzer>(main) CZEPMySQLManagerException: " + e.getMessage( ) + " - failed to connect" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CAnalyzer>(main) aborted" );
            return;            
        }
        
        try
        {
            //ds log cutoffs
            //logCutoffs( "cutoffs.csv" );
            
            //ds log tags liked and disliked
            logLearnerTagFrequencies( "tags_liked.csv", "tags_disliked.csv" );
            
            /*ds user names to log
            Vector< String > vecUsernames = new Vector< String >( 8 );
            vecUsernames.add( "Isabellinska" );
            vecUsernames.add( "judas" );
            vecUsernames.add( "themanuuu" );
            vecUsernames.add( "juitto" );
            vecUsernames.add( "ProProcrastinator" );
            vecUsernames.add( "carnage" );
            vecUsernames.add( "ChillyOnMyWilly" );
            vecUsernames.add( "Glaus" );
            vecUsernames.add( "memyself" );
            
            //ds create the csvs for the users
            logFromLearner( vecUsernames );*/
        }
        catch( Exception e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CAnalyzer>(main) Exception: " + e.getMessage( ) + " - badoing" );            
        }
        
        
        //ds exit and close jvm
        System.out.println( "[" + CLogger.getStamp( ) + "]<CAnalyzer>(main) terminated" ); 
        System.exit( 0 );
        return;
    }

    public static void logCutoffs( final String p_strFilename ) throws SQLException, IOException
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CAnalyzer>(logCutoffs) Computing cutoff values .." );   
        
        //ds get cutoff values
        Vector< Integer > vecCutoffValues = m_cMySQLManager.getCutoffValues( );
        
        //ds sort decreasing
        Collections.sort( vecCutoffValues, Collections.reverseOrder( ) );
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CAnalyzer>(logCutoffs) Acquiring tag types per cutoff .." );
        
        //ds map for tag numbers
        Map< Integer, Integer > mapTagsPerCutoff = new HashMap< Integer, Integer >( vecCutoffValues.size( ) );
        
        //ds we now have to retrieve the number of tag types for each cutoff setting
        for( int iCutoff: vecCutoffValues )
        {
            //ds get number of tag types for this cutoff
            mapTagsPerCutoff.put( iCutoff, m_cMySQLManager.getNumberOfTagTypesForCutoff( iCutoff ) );
        }
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CAnalyzer>(logCutoffs) Acquiring datapoints per cutoff .." );
        
        //ds get datapoints per cutoff
        Map< Integer, Integer > mapDataPointsPerCutoff = m_cMySQLManager.getNumberOfDataPointsForCutoffs( vecCutoffValues );
        
        //ds final accumulated map
        Map< Integer, Integer > mapDataPointsPerCutoffAccumulated = new HashMap< Integer, Integer >( mapDataPointsPerCutoff );
        
        //ds processed values
        int iProcessed = 0;
        
        //ds we now have to accumulate the counts for all lower frequencies
        for( int iCutoff: vecCutoffValues )
        {
            //ds get the value at the top
            final int iCountDataPoints = mapDataPointsPerCutoff.get( iCutoff );
            
            //ds one processed
            ++iProcessed;
            
            //ds add it to all the other entries below
            for( int i = iProcessed; i < vecCutoffValues.size( ); ++i )
            {
                //ds get the cutoff
                final int iCutoffCurrent = vecCutoffValues.get( i );
                
                //ds update final map
                mapDataPointsPerCutoffAccumulated.put( iCutoffCurrent, mapDataPointsPerCutoffAccumulated.get( iCutoffCurrent )+iCountDataPoints );
            }
        }
        
        //ds reorder cutoffs for printing - increasing
        Collections.sort( vecCutoffValues );        
        
        //ds open writer
        FileWriter cWriter = new FileWriter( p_strFilename, false );
        
        //ds append entry for each cutoff
        for( int iCutoff: vecCutoffValues )
        {
            cWriter.append( iCutoff + "," + mapTagsPerCutoff.get( iCutoff ) + "," + mapDataPointsPerCutoffAccumulated.get( iCutoff ) + "\n" );
        }
        
        //ds close writer
        cWriter.close( );
       
        System.out.println( "[" + CLogger.getStamp( ) + "]<CAnalyzer>(logCutoffs) Computation complete" );           
    }
    
    public static void logFromLearner( final Vector< String > p_vecUsernames ) throws SQLException, IOException
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CAnalyzer>(logCutoffs) Retrieving datapoint history for users: " + p_vecUsernames ); 
        
        //ds for each user
        for( String strUsername: p_vecUsernames )
        {
            //ds call logger
            m_cMySQLManager.writeCSVFromLogLearner( strUsername );
        }
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CAnalyzer>(logCutoffs) learner log evaluation complete" );           
    }
    
    public static void logLearnerTagFrequencies( final String p_strFilenameLiked, final String p_strFilenameDisliked ) throws SQLException, IOException, CZEPMySQLManagerException
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CAnalyzer>(logLearnerTagFrequencies) Aqcuiring tag information from learner table .." );  
        
        //ds map with tag names and frequency
        Vector< CPair< String, Double > > vecTagFrequenciesLiked    = m_cMySQLManager.getLearnerTagFrequenciesNormalized( "cutoff_104_tags", true );
        Vector< CPair< String, Double > > vecTagFrequenciesDisliked = m_cMySQLManager.getLearnerTagFrequenciesNormalized( "cutoff_104_tags", false );
        
        //ds sort maps by frequencies, descending
        Collections.sort( vecTagFrequenciesLiked, new CPair.CComparatorDecreasing( ) );
        Collections.sort( vecTagFrequenciesDisliked, new CPair.CComparatorDecreasing( ) );
        
        //ds open writers
        FileWriter cWriterLiked    = new FileWriter( p_strFilenameLiked, false );
        FileWriter cWriterDisliked = new FileWriter( p_strFilenameDisliked, false );
        
        //ds write vectors to file
        for( CPair< String, Double > cTag: vecTagFrequenciesLiked )
        {
            cWriterLiked.append( cTag.A + "," + cTag.B + "\n" );
        }
        for( CPair< String, Double > cTag: vecTagFrequenciesDisliked )
        {
            cWriterDisliked.append( cTag.A + "," + cTag.B + "\n" );
        }
        
        //ds close writer
        cWriterLiked.close( );
        cWriterDisliked.close( );
       
        System.out.println( "[" + CLogger.getStamp( ) + "]<CAnalyzer>(logLearnerTagFrequencies) Logging complete" );           
    }
}
