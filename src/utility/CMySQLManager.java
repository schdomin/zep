package utility;

import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

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
    private final int m_iMySQLTimeout = 10000;
    
    //ds log file writers
    private String m_strLogFileFeatureGrowth = "features-";
    
    //ds constructor
    public CMySQLManager( final String p_strMySQLDriver, final String p_strMySQLServerURL, final String p_strMySQLUsername, final String p_strMySQLPassword )
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(CMySQLManager) Instance allocated" );
        
        //ds fix the logfile name
        m_strLogFileFeatureGrowth += CLogger.getFileStamp( ) + ".csv";
        
        //ds set values
        m_strMySQLDriver    = p_strMySQLDriver;
        m_strMySQLServerURL = p_strMySQLServerURL;
        m_strMySQLUsername  = p_strMySQLUsername;
        m_strMySQLPassword  = p_strMySQLPassword;
    }
    
    //ds launch - establishes connection
    public final void launch( ) throws ClassNotFoundException, SQLException, CZEPMySQLManagerException
    {
        //ds setup driver
        Class.forName( m_strMySQLDriver );
        
        //ds set login timeout
        DriverManager.setLoginTimeout( 10 );
        
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
    public final boolean isEmpty( ) throws SQLException, CZEPMySQLManagerException
    {
        //ds max id is zero for empty table
        return ( 0 == _getMaxIDFromTable( "id_datapoint", "datapoints" ) );
    }
    
    //ds insert datapoint
    public final void insertDataPoint( final CDataPoint p_cDataPoint ) throws SQLException, CZEPMySQLManagerException, MalformedURLException, IOException
    {
        //ds first check if we already have an entry for this image (no double URLs allowed)
        final PreparedStatement cStatementCheckDataPoint = m_cMySQLConnection.prepareStatement( "SELECT `id_datapoint` from `datapoints` WHERE `url` = ( ? ) LIMIT 1" );
        cStatementCheckDataPoint.setString( 1, p_cDataPoint.getURL( ).toString( ) );
        
        //ds if there is no entry yet
        if( !cStatementCheckDataPoint.executeQuery( ).next( ) )
        {
            //ds add the datapoint to the SQL database
            final PreparedStatement cStatementInsertDataPoint = m_cMySQLConnection.prepareStatement( "INSERT IGNORE INTO `datapoints` " +
            		                                                                                 "(`url`, `title`, `type`, `likes`, `dislikes`, `count_comments`, `count_tags`, " +
            		                                                                                 "`is_photo`, `text_amount` ) VALUE ( ?, ?, ?, ?, ?, ?, ?, ?, ? )" );
            cStatementInsertDataPoint.setString( 1, p_cDataPoint.getURL( ).toString( ) );
            cStatementInsertDataPoint.setString( 2, p_cDataPoint.getTitle( ) );
            cStatementInsertDataPoint.setString( 3, p_cDataPoint.getType( ) );
            cStatementInsertDataPoint.setInt( 4, p_cDataPoint.getLikes( ) );
            cStatementInsertDataPoint.setInt( 5, p_cDataPoint.getDislikes( ) );
            cStatementInsertDataPoint.setInt( 6, p_cDataPoint.getCountComments( ) );
            cStatementInsertDataPoint.setInt( 7, p_cDataPoint.getCountTags( ) );
            cStatementInsertDataPoint.setBoolean( 8, p_cDataPoint.isPhoto( ) );
            cStatementInsertDataPoint.setDouble( 9, p_cDataPoint.getTextAmount( ) );
            cStatementInsertDataPoint.executeUpdate( );          
            
            //ds obtain the datapoint id
            final ResultSet cResultDataPoint = cStatementCheckDataPoint.executeQuery( );
            
            //ds this must work now
            if( !cResultDataPoint.next( ) ){ throw new CZEPMySQLManagerException( "from DataPoint: could not establish datapoint - feature linkage" ); }
            
            //ds get the id
            final int iID_DataPoint = cResultDataPoint.getInt( "id_datapoint" );
            
            //ds add the image file to the database
            final PreparedStatement cStatementInsertImage = m_cMySQLConnection.prepareStatement( "INSERT IGNORE INTO `images` ( `id_datapoint`, `data_binary` ) VALUE ( ?, ? )" );
            cStatementInsertImage.setInt( 1, iID_DataPoint );
            cStatementInsertImage.setBlob( 2, new URL( p_cDataPoint.getURL( ).toString( ) ).openStream( ) );
            cStatementInsertImage.executeUpdate( );
            
            //ds logging last feature id
            int iID_FeatureCurrent = _getMaxIDFromTable( "id_feature", "features" );
            
            //ds then add the tags to the features table
            for( String strTag : p_cDataPoint.getTags( ) )
            {
            	//ds for each tag check if we already have an entry in the features map
                final PreparedStatement cStatementCheckFeatures = m_cMySQLConnection.prepareStatement( "SELECT * from `features` WHERE `value` = ( ? ) LIMIT 1" );
                cStatementCheckFeatures.setString( 1, strTag );
                
                //ds get the result
                final ResultSet cResultFeature = cStatementCheckFeatures.executeQuery( );
                
                //ds if there is no entry yet
                if( !cResultFeature.next( ) )
                {
                	//ds add the tag to the features table
                    final PreparedStatement cStatementInsertFeature = m_cMySQLConnection.prepareStatement( "INSERT IGNORE INTO `features` ( `value`, `frequency` ) VALUE ( ?, ? )" );
                    cStatementInsertFeature.setString( 1, strTag );
                    cStatementInsertFeature.setInt( 2, 1 );
                    cStatementInsertFeature.executeUpdate( );
                    
                    //ds increment feature counter
                    ++iID_FeatureCurrent;
                }
                else
                {
                	//ds increment the frequency counter - first obtain it
                    final int iCounter = cResultFeature.getInt( "frequency" );
                    
                    //ds insert the incremented counter
                    final PreparedStatement cStatementInsertFeature = m_cMySQLConnection.prepareStatement( "UPDATE `features` SET `frequency` = ( ? ) WHERE `value` = ( ? )" );
                    cStatementInsertFeature.setInt( 1, iCounter+1 );
                    cStatementInsertFeature.setString( 2, strTag );
                    cStatementInsertFeature.executeUpdate( );                    
                }
                
                //ds now we have to create the linkage between features and datapoints over the patterns table - execute the query for the tag again
                final ResultSet cResultFeatureID = cStatementCheckFeatures.executeQuery( );
                
                //ds this must work now
                if( !cResultFeatureID.next( ) ){ throw new CZEPMySQLManagerException( "from Feature: could not establish datapoint - feature linkage" ); }
                
                //ds obtain the feature id
                final int iID_Feature = cResultFeatureID.getInt( "id_feature" );
                
                //ds establish the linkage between id_datapoint and id_feature
                final PreparedStatement cStatementInsertLinkage = m_cMySQLConnection.prepareStatement( "INSERT IGNORE INTO `mappings` ( `id_datapoint`, `id_feature` ) VALUE ( ?, ? )" );
                cStatementInsertLinkage.setInt( 1, iID_DataPoint );
                cStatementInsertLinkage.setInt( 2, iID_Feature );
                cStatementInsertLinkage.executeUpdate( );
            }
            
            //ds get current total feature number (simply the size of the mappings table)
            final int iCurrentTotalFeatures = _getMaxIDFromTable( "id_mapping", "mappings" );
            
            //ds open writer - locally because we want the data written in case the process dies unexpectedly
            FileWriter writer = new FileWriter( m_strLogFileFeatureGrowth, true );
            
            //ds append entry
            writer.append( iID_DataPoint + "," + iID_FeatureCurrent + "," + iCurrentTotalFeatures + "\n" );
            
            //ds close writer
            writer.close( );
        }
        else
        {
            throw new CZEPMySQLManagerException( "element already in database" );
        }
    }
    
    //ds access function: single datapoint by id
    public final CDataPoint getDataPointByID( final int p_iID_DataPoint ) throws SQLException, MalformedURLException, CZEPMySQLManagerException
    {
        //ds query for the id
        final PreparedStatement cRetrieveDataPoint = m_cMySQLConnection.prepareStatement( "SELECT * FROM `datapoints` WHERE `id_datapoint` = ( ? ) LIMIT 1" );
        cRetrieveDataPoint.setInt( 1, p_iID_DataPoint );
        
        //ds get the result
        final ResultSet cResultSetDataPoint = cRetrieveDataPoint.executeQuery( );
        
        //ds if we could access the datapoint
        if( cResultSetDataPoint.next( ) )
        {
            //ds return the datapoint
            return _getDataPointFromResultSet( cResultSetDataPoint );    
        }
        else
        {
            throw new CZEPMySQLManagerException( "could not retrieve datapoint from MySQL database - ID: " + p_iID_DataPoint );
        }
    }
    
    //ds fetches a set of datapoints
    public final Vector< CDataPoint > getDataPointsByIDRange( final int p_iIDStart, final int p_iIDEnd ) throws SQLException, MalformedURLException, CZEPMySQLManagerException
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(getDataPointsByIDRange) Received fetch request for: [" + p_iIDStart + "," + p_iIDEnd + "]" );
        
        //ds get max id in database
        final int iIDMaximum = _getMaxIDFromTable( "id_datapoint", "datapoints" );
        
        //ds check if we want to access to much
        if( p_iIDEnd > iIDMaximum )
        {
            //ds escape
            throw new CZEPMySQLManagerException( "index: " + p_iIDEnd + " is out of range: " + iIDMaximum );            
        }
            
        //ds target number of elements to fetch
        final int iFetchSize = p_iIDEnd-p_iIDStart+1;
            
        //ds vector to fill
        final Vector< CDataPoint > vecDataPoints = new Vector< CDataPoint >( 0 );
        
        //ds query for the id
        final PreparedStatement cRetrieveDataPoint = m_cMySQLConnection.prepareStatement( "SELECT * FROM `datapoints` WHERE `id_datapoint` >= ( ? )" );
        cRetrieveDataPoint.setInt( 1, p_iIDStart );
        
        //ds get the result
        final ResultSet cResultSetDataPoint = cRetrieveDataPoint.executeQuery( );
        
        //ds as long as we have remaining data and do not exceed the fetch size
        while( cResultSetDataPoint.next( ) && iFetchSize > vecDataPoints.size( ) )
        {
            //ds add the datapoint to the vector
            vecDataPoints.add( _getDataPointFromResultSet( cResultSetDataPoint ) );        
        }
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(getDataPointsByIDRange) Number of points fetched: " + vecDataPoints.size( ) );
        
        //ds check first and last id
        if( p_iIDStart != vecDataPoints.firstElement( ).getID( ) || p_iIDEnd != vecDataPoints.lastElement( ).getID( ) )
        {
            //ds escape
            throw new CZEPMySQLManagerException( "fetching mismatch for datapoint range: [" + p_iIDStart + "," + p_iIDEnd + "]" );
        }
        
        //ds return the elements
        return vecDataPoints;
    }
    
    //ds access function: single datapoint by feature
    public CDataPoint getDataPointByFeature( final String p_strTag )
    {
        //ds TODO
        return null;
    }
    
    //ds regular image access
    public final BufferedImage getBufferedImage( final CDataPoint p_cDataPoint ) throws CZEPMySQLManagerException
    {
        try
        {
            //ds grab the image row
            final PreparedStatement cRetrieveImage = m_cMySQLConnection.prepareStatement( "SELECT * FROM `images` WHERE `id_datapoint` = ( ? ) LIMIT 1" );
            cRetrieveImage.setInt( 1, p_cDataPoint.getID( ) );

            //ds execute the statement
            final ResultSet cResultSetImage = cRetrieveImage.executeQuery( );

            //ds if we got something
            if( cResultSetImage.next( ) )
            {
                //ds get the image
                final BufferedImage cImage = ImageIO.read( cResultSetImage.getBlob( "data_binary" ).getBinaryStream( ) );

                //ds return
                return cImage;
            }
            else
            {
                throw new CZEPMySQLManagerException( "could not load image from MySQL database - ID: " + p_cDataPoint.getID( ) );
            }
        }
        catch( SQLException e )
        {
            throw new CZEPMySQLManagerException( "SQLException: " + e.getMessage( ) + " could not load image from MySQL database - ID: " + p_cDataPoint.getID( ) );
        }
        catch( IOException e )
        {
            throw new CZEPMySQLManagerException( "IOException: " + e.getMessage( ) + " could not load image from MySQL database - ID: " + p_cDataPoint.getID( ) );
        }
    }

    //ds gif image access
    public final ImageIcon getImageIcon( final CDataPoint p_cDataPoint ) throws CZEPMySQLManagerException
    {
        try
        {
            //ds grab the image row
            final PreparedStatement cRetrieveImage = m_cMySQLConnection.prepareStatement( "SELECT * FROM `images` WHERE `id_datapoint` = ( ? ) LIMIT 1" );
            cRetrieveImage.setInt( 1, p_cDataPoint.getID( ) );
        
            //ds execute the statement
            final ResultSet cResultSetImage = cRetrieveImage.executeQuery( );
        
            //ds if we got something
            if( cResultSetImage.next( ) )
            {
                //ds get the blob from mysql
                final Blob cBlob =  cResultSetImage.getBlob( "data_binary" );
        
                //ds read the file into a image icon
                final ImageIcon cImage = new ImageIcon( cBlob.getBytes( 1, ( int )cBlob.length( ) ) );
        
                //ds return it
                return cImage;
            }
            else
            {
                throw new CZEPMySQLManagerException( "could not load image from MySQL database - ID: " + p_cDataPoint.getID( ) );
            }
        }
        catch( SQLException e )
        {
            throw new CZEPMySQLManagerException( "SQLException: " + e.getMessage( ) + " could not load image from MySQL database - ID: " + p_cDataPoint.getID( ) );    		
        }
    }
    
    //ds access function: multiple datapoints
    public final Map< Integer, CDataPoint > getDataset( final int p_iMaximumNumberOfDataPoints ) throws SQLException, MalformedURLException
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(getDataset) received fetch request - start downloading .." );
        
        //ds informative counter
        int iNumberOfDataPoints = 0;
        
        //ds allocate a fresh map
        final Map< Integer, CDataPoint > mapDataset = new HashMap< Integer, CDataPoint >( );
        
        //ds query for the first id
        final PreparedStatement cRetrieveDataPoint = m_cMySQLConnection.prepareStatement( "SELECT * FROM `datapoints` WHERE `id_datapoint` >= 1" );
        
        //ds get the result
        final ResultSet cResultSetDataPoint = cRetrieveDataPoint.executeQuery( );
        
        //ds as long as we have remaining data
        while( cResultSetDataPoint.next( ) && iNumberOfDataPoints < p_iMaximumNumberOfDataPoints )
        {
            //ds get the datapoint
            final CDataPoint cDataPoint = _getDataPointFromResultSet( cResultSetDataPoint );
            
            //ds add the datapoint to the map
            mapDataset.put( cDataPoint.getID( ), cDataPoint );
            
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
    
    //ds helper for emptyness check
    private final int _getMaxIDFromTable( final String p_strKeyID, final String p_strTable ) throws SQLException, CZEPMySQLManagerException
    {
        //ds determine the current max id in the database
        final PreparedStatement cStatementCheck = m_cMySQLConnection.prepareStatement( "SELECT MAX(`"+p_strKeyID+"`) FROM `"+p_strTable+"`" );
        final ResultSet cResultSetCheck         = cStatementCheck.executeQuery( );
        
        //ds default
        int iMaxID = -1;
        
        //ds extract the value
        while( cResultSetCheck.next( ) )
        {
            //ds set the max id
            iMaxID = cResultSetCheck.getInt( "MAX(`"+p_strKeyID+"`)" );
        }
        
        //ds internal SQL error if still -1
        if( -1 == iMaxID )
        {
            throw new CZEPMySQLManagerException( "could not retrieve max ID" );
        }
        
        return iMaxID;
    }
    
    //ds helper for dataelement retrieval
    private final CDataPoint _getDataPointFromResultSet( final ResultSet p_cResultSetDataPoint ) throws SQLException, MalformedURLException
    {
        //ds get the ID
        final int iID_DataPoint = p_cResultSetDataPoint.getInt( "id_datapoint" );
        
        //ds extract the data separately (for readability)
        final String strURL      = p_cResultSetDataPoint.getString( "url" );
        final String strTitle    = p_cResultSetDataPoint.getString( "title" );
        final String strType     = p_cResultSetDataPoint.getString( "type" );
        final int iLikes         = p_cResultSetDataPoint.getInt( "likes" );
        final int iDislikes      = p_cResultSetDataPoint.getInt( "dislikes" );
        final int iCountComments = p_cResultSetDataPoint.getInt( "count_comments" );
        final int iCountTags     = p_cResultSetDataPoint.getInt( "count_tags" );
        final boolean bIsPhoto   = p_cResultSetDataPoint.getBoolean( "is_photo" );
        final double dTextAmount = p_cResultSetDataPoint.getDouble( "text_amount" );
        
        //ds tag vector to be filled
        final Vector< String > vecTags = new Vector< String >( );
        
        //ds now retrieve the feature information (tags)
        final PreparedStatement cRetrieveMapping = m_cMySQLConnection.prepareStatement( "SELECT * FROM `mappings` WHERE `id_datapoint` = ( ? )" );
        cRetrieveMapping.setInt( 1, iID_DataPoint );
        
        //ds get the mapping
        final ResultSet cResultSetMapping = cRetrieveMapping.executeQuery( );
        
        //ds for all the mappings
        while( cResultSetMapping.next( ) )
        {
            //ds get feature id
            final int iID_Feature = cResultSetMapping.getInt( "id_feature" );
       
            //ds get actual feature
            final PreparedStatement cRetrieveTag = m_cMySQLConnection.prepareStatement( "SELECT * FROM `features` WHERE `id_feature` = ( ? ) LIMIT 1" );
            cRetrieveTag.setInt( 1, iID_Feature );
            
            //ds get the feature
            final ResultSet cResultSetTag = cRetrieveTag.executeQuery( );
            
            //ds if exists
            if( cResultSetTag.next( ) )
            {
                //ds add the current tag
                vecTags.add( cResultSetTag.getString( "value" ) );
            }
        }
        
        //ds return the datapoint
        return new CDataPoint( iID_DataPoint, new URL( strURL ), strTitle, strType, iLikes, iDislikes, iCountComments, iCountTags, bIsPhoto, dTextAmount, vecTags );
    }
}
