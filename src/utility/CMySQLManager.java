package utility;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

//ds custom imports
import exceptions.CZEPMySQLManagerException;

public final class CMySQLManager
{
    //ds soft attributes
    private final String m_strMySQLDriver;
    private final String m_strMySQLServerURL;
    private final String m_strMySQLUsername;
    private final String m_strMySQLPassword;
    
    //ds connection object
    private Connection m_cMySQLConnection;
    
    //ds fixed values (for now)
    final private int m_iMySQLTimeout = 10000;
    
    //ds constructor
    public CMySQLManager( final String p_strMySQLDriver, final String p_strMySQLServerURL, final String p_strMySQLUsername, final String p_strMySQLPassword )
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(CMySQLManager) CMySQLManager instance allocated" );
        
        //ds set values
        m_strMySQLDriver    = p_strMySQLDriver;
        m_strMySQLServerURL = p_strMySQLServerURL;
        m_strMySQLUsername  = p_strMySQLUsername;
        m_strMySQLPassword  = p_strMySQLPassword;
    }
    
    //ds launch - establishes connection
    public void launch( ) throws ClassNotFoundException, SQLException, CZEPMySQLManagerException
    {
        //ds setup driver
        Class.forName( m_strMySQLDriver );
        
        //ds establish connection
        m_cMySQLConnection = DriverManager.getConnection( m_strMySQLServerURL, m_strMySQLUsername, m_strMySQLPassword );      
        
        //ds check connection
        if( !m_cMySQLConnection.isValid( m_iMySQLTimeout ) )
        {
            //ds escape
            throw new CZEPMySQLManagerException( "timeout (" + m_iMySQLTimeout +  " ms) reached during connectivity test" );
        }
    }
    
    //ds check for emptyness
    public boolean isEmpty( ) throws SQLException, CZEPMySQLManagerException
    {
        //ds max id is zero for empty table
        return ( 0 == _getMaxIDFromTable( ) );
    }
    
    //ds insert datapoint
    public void insertDataPoint( final CDataPoint p_cDataPoint ) throws SQLException, CZEPMySQLManagerException, MalformedURLException, IOException
    {
        //ds first check if we already have an entry for this image (no double URLs allowed)
        final PreparedStatement cStatementCheckDataPoint = m_cMySQLConnection.prepareStatement( "SELECT `id_datapoint` from `datapoints` WHERE (`url`) = (?) LIMIT 1" );
        cStatementCheckDataPoint.setString( 1, p_cDataPoint.getURL( ).toString( ) );
        
        //ds if there is no entry yet
        if( !cStatementCheckDataPoint.executeQuery( ).next( ) )
        {
            //ds add the datapoint to the SQL database
            final PreparedStatement cStatementInsertDataPoint = m_cMySQLConnection.prepareStatement( "INSERT IGNORE INTO `datapoints` (`url`, `title`, `type`) VALUE (?,?,?)" );
            cStatementInsertDataPoint.setString( 1, p_cDataPoint.getURL( ).toString( ) );
            cStatementInsertDataPoint.setString( 2, p_cDataPoint.getTitle( ) );
            cStatementInsertDataPoint.setString( 3, p_cDataPoint.getType( ) );
            cStatementInsertDataPoint.executeUpdate( );          
            
            //ds obtain the datapoint id
            final ResultSet cResultDataPoint = cStatementCheckDataPoint.executeQuery( );
            
            //ds this must work now
            if( !cResultDataPoint.next( ) ){ throw new CZEPMySQLManagerException( "from DataPoint: could not establish datapoint - feature linkage" ); }
            
            //ds get the id
            final int iID_DataPoint = cResultDataPoint.getInt( "id_datapoint" );
            
            //ds add the image file to the database
            final PreparedStatement cStatementInsertImage = m_cMySQLConnection.prepareStatement( "INSERT IGNORE INTO `images` (`id_datapoint`, `file`) VALUE (?,?)" );
            cStatementInsertImage.setInt( 1, iID_DataPoint );
            cStatementInsertImage.setBlob( 2, new URL( p_cDataPoint.getURL( ).toString( ) ).openStream( ) );
            cStatementInsertImage.executeUpdate( );
            
            //ds then add the tags to the features table
            for( String strTag : p_cDataPoint.getTags( ) )
            {
            	//ds for each tag check if we already have an entry in the features map
                final PreparedStatement cStatementCheckFeatures = m_cMySQLConnection.prepareStatement( "SELECT `id_feature` from `features` WHERE (`value`) = (?) LIMIT 1" );
                cStatementCheckFeatures.setString( 1, strTag );
                
                //ds if there is no entry yet
                if( !cStatementCheckFeatures.executeQuery( ).next( ) )
                {
                	//ds add the tag to the features table
                    final PreparedStatement cStatementInsertFeature = m_cMySQLConnection.prepareStatement( "INSERT INTO `features` (`value`) VALUE (?)" );
                    cStatementInsertFeature.setString( 1, strTag );
                    cStatementInsertFeature.executeUpdate( );
                }
                
                //ds now we have to create the linkage between features and datapoints over the patterns table - execute the query for the tag again
                final ResultSet cResultFeature = cStatementCheckFeatures.executeQuery( );
                
                //ds this must work
                if( !cResultFeature.next( ) ){ throw new CZEPMySQLManagerException( "from Feature: could not establish datapoint - feature linkage" ); }
                
                //ds obtain the feature id
                final int iID_Feature = cResultFeature.getInt( "id_feature" );
                
                //ds establish the linkage between id_datapoint and id_feature
                final PreparedStatement cStatementInsertLinkage = m_cMySQLConnection.prepareStatement( "INSERT IGNORE INTO `mappings` (`id_datapoint`, `id_feature`) VALUE (?,?)" );
                cStatementInsertLinkage.setInt( 1, iID_DataPoint );
                cStatementInsertLinkage.setInt( 2, iID_Feature );
                cStatementInsertLinkage.executeUpdate( );
            }
        }
        else
        {
            throw new CZEPMySQLManagerException( "element already in database" );
        }
    }
    
    //ds access function
    public Map< Integer, CDataPoint > getDataset( ) throws SQLException, MalformedURLException
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(getDataset) received fetch request - start downloading .." );
        
        //ds informative counters
        int iNumberOfDataPoints = 0;
        int iNumberOfFeatures   = 0;
        
    	//ds allocate a fresh map
    	final Map< Integer, CDataPoint > mapDataset = new HashMap< Integer, CDataPoint >( );
    	
        //ds query for the first id
        final PreparedStatement cRetrieveDataPoint = m_cMySQLConnection.prepareStatement( "SELECT * FROM `datapoints` WHERE `id_datapoint` >= 1" );
        
        //ds get the result
        final ResultSet cResultSetDataPoint = cRetrieveDataPoint.executeQuery( );
        
        //ds as long as we have remaining data
        while( cResultSetDataPoint.next( ) )
        {   
        	//ds get the ID
        	final int iID_DataPoint = cResultSetDataPoint.getInt( "id_datapoint" );
        	
            //ds extract the data separately (for readability)
            final String strURL   = cResultSetDataPoint.getString( "url" );
            final String strTitle = cResultSetDataPoint.getString( "title" );
            final String strType  = cResultSetDataPoint.getString( "type" );
            
            //ds tag vector to be filled
            Vector< String > vecTags = new Vector< String >( );
            
            //ds now retrieve the feature information (tags)
            final PreparedStatement cRetrieveMapping = m_cMySQLConnection.prepareStatement( "SELECT * FROM `mappings` WHERE (`id_datapoint`) = (?)" );
            cRetrieveMapping.setInt( 1, iID_DataPoint );
            
            //ds get the mapping
            final ResultSet cResultSetMapping = cRetrieveMapping.executeQuery( );
            
            //ds for all the mappings
            while( cResultSetMapping.next( ) )
            {
            	//ds get feature id
            	final int iID_Feature = cResultSetMapping.getInt( "id_feature" );
            	
            	//ds get actual feature
                final PreparedStatement cRetrieveTag = m_cMySQLConnection.prepareStatement( "SELECT * FROM `features` WHERE (`id_feature`) = (?) LIMIT 1" );
                cRetrieveTag.setInt( 1, iID_Feature );
                
                //ds get the feature
                final ResultSet cResultSetTag = cRetrieveTag.executeQuery( );
                
            	//ds if exists
            	if( cResultSetTag.next( ) )
            	{
            		//ds add the current tag
            		vecTags.add( cResultSetTag.getString( "value" ) );
            		
            		//ds update counter
            		++iNumberOfFeatures;
            	}
            }
            
            //ds add the datapoint to the map
            mapDataset.put( iID_DataPoint, new CDataPoint( iID_DataPoint, new URL( strURL ), strTitle, strType, vecTags ) );
            
            //ds update
            ++iNumberOfDataPoints;
            
            //ds log for every 100 imports
            if( 0 == iNumberOfDataPoints%100 )
            {
            	System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(getDataset) fetched: " + iNumberOfDataPoints + " DataPoints and continuing .." );
            }
        }
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(getDataset) fetching complete" );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(getDataset) added : " + iNumberOfDataPoints + " DataPoints" );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(getDataset) with  : " + iNumberOfFeatures + " Features" );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(getDataset) ratio : " + ( float )iNumberOfFeatures/iNumberOfDataPoints );
        
        return mapDataset;
    }
    
    /*ds updater function
    public void updateDataset( Map< Integer, CDataPoint > p_mapFeatureData ) throws SQLException
    {
        //ds get the last id of the current feature data (1 for 1 element and so on) - we add one to access the next id in the mysql database
        final int iLastID = p_mapFeatureData.size( )+1;
        
        //ds default max id (no update if not greater equal than last id
        int iMaxID = iLastID-1;
        
        try
        {
            //ds try to get the max id from the table
            iMaxID = _getMaxIDFromTable( );
        }
        catch( CZEPMySQLManagerException e )
        {
            //ds not fatal
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(updateDataset) CZEPMySQLManagerException: " + e.getMessage( ) + " - could not retrieve max ID from table" );
        }
        
        //ds check if import is possible/necessary
        if( iLastID <= iMaxID )
        {
            //ds query for the first id
            final PreparedStatement cStatement = m_cMySQLConnection.prepareStatement( "SELECT `url`, `title`, `type`, `tags` FROM `features` WHERE `id` >= ?" );
            cStatement.setInt( 1, iLastID );
            
            //ds get the result
            final ResultSet cResultSet = cStatement.executeQuery( );
            
            //ds counter
            int iIDCounter = 0;
            
            //ds as long as we have remaining data
            while( cResultSet.next( ) )
            {
                //ds set the id
                final int iID = iLastID+iIDCounter;
                
                //ds increment counter
                ++iIDCounter;
                
                //ds extract the data (for readability)
                final String strURL   = cResultSet.getString( "url" );
                final String strTitle = cResultSet.getString( "title" );
                final String strType  = cResultSet.getString( "type" );
                final String strTags  = cResultSet.getString( "tags" );
                
                try
                {
                    //ds parse an actual vector of the string data
                    final Vector< String > vecTags = CConverter.parseVector( strTags );
                
                    //ds add the datapoint to the map
                    p_mapFeatureData.put( iID, new CDataPoint( iID, strURL, strTitle, strType, vecTags ) );
                }
                catch( CZEPConversionException e )
                {
                    //ds the particular datapoint couldnt be added - not fatal
                    System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(updateDataset) CZEPConversionException: " + e.getMessage( ) + " - unable to add datapoint: " + strURL );
                }
            }
            
            //ds check if update failed
            if( iMaxID != p_mapFeatureData.size( ) )
            {
                //ds only log - not fatal
                System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(updateDataset) unable to fetch all elements from " + iLastID + " to " + iMaxID );
            }
            else
            {
                System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(updateDataset) succcessfully fetched " + (  iMaxID-iLastID+1 ) + " elements" );
            }
        }
        else
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(updateDataset) no new elements available - update terminated" );
        }
    }*/
    
    private final int _getMaxIDFromTable( ) throws SQLException, CZEPMySQLManagerException
    {
        //ds determine the current max id in the database
        final PreparedStatement cStatementCheck = m_cMySQLConnection.prepareStatement( "SELECT MAX(`id_datapoint`) FROM `datapoints`" );
        final ResultSet cResultSetCheck         = cStatementCheck.executeQuery( );
        
        //ds default
        int iMaxID = -1;
        
        //ds extract the value
        while( cResultSetCheck.next( ) )
        {
            //ds set the max id
            iMaxID = cResultSetCheck.getInt( "MAX(`id_datapoint`)" );
        }
        
        //ds internal SQL error if still -1
        if( -1 == iMaxID )
        {
            throw new CZEPMySQLManagerException( "could not retrieve max ID" );
        }
        
        return iMaxID;
    }
}
