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

import utility.CTag;

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
    private final int m_iMySQLTimeoutMS = 10000;
    
    //ds log file writers
    private String m_strLogFileTagGrowth = "tags-";
    
    //ds feature handling
    private final double m_dMinimumText               = 0.25;
    private final double m_dMinimumLikeDislikeRatio   = 5;
    private final double m_dMaximumVotesCommentsRatio = 10;
    
    //ds constructor
    public CMySQLManager( final String p_strMySQLDriver, final String p_strMySQLServerURL, final String p_strMySQLUsername, final String p_strMySQLPassword )
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(CMySQLManager) Instance allocated" );
        
        //ds fix the logfile name
        m_strLogFileTagGrowth += CLogger.getFileStamp( ) + ".csv";
        
        //ds set values
        m_strMySQLDriver    = p_strMySQLDriver;
        m_strMySQLServerURL = p_strMySQLServerURL;
        m_strMySQLUsername  = p_strMySQLUsername;
        m_strMySQLPassword  = p_strMySQLPassword;
    }
    
    //ds standalone
    public static void main( String[] args )
    {
        //ds default configuration parameters: mysql
        final String strMySQLDriver    = "com.mysql.jdbc.Driver";
        final String strMySQLServerURL = "jdbc:mysql://pc-10129.ethz.ch:3306/domis";
        final String strMySQLUsername  = "domis";
        final String strMySQLPassword  = "N0effort";
        
        //ds allocate the MySQL manager
        final CMySQLManager cMySQLManager = new CMySQLManager( strMySQLDriver, strMySQLServerURL, strMySQLUsername, strMySQLPassword );
        
        try
        {
            //ds launch the manager
            cMySQLManager.launch( );
        }
        catch( ClassNotFoundException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(main) ClassNotFoundException: " + e.getMessage( ) + " - could not establish MySQL connection" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(main) aborted" );
            return;
        }
        catch( SQLException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(main) SQLException: " + e.getMessage( ) + " - could not establish a valid MySQL connection" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(main) aborted" );
            return;            
        }
        catch( CZEPMySQLManagerException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(main) CZEPMySQLManagerException: " + e.getMessage( ) + " - failed to connect" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(main) aborted" );
            return;            
        }
        
        try
        {
            //ds create feature table
            cMySQLManager.createPatternsTable( 10, 1 );
            
        }
        catch( Exception e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(main) Exception: " + e.getMessage( ) + " - failed" );           
        }
        try
        {
            //ds compute probabilities
            cMySQLManager.computeProbabilities( 10 );
            
        }
        catch( Exception e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(main) Exception: " + e.getMessage( ) + " - failed" );
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(main) aborted" );
            return;             
        }
        
        return;
    }
    
    //ds launch - establishes connection
    public final void launch( ) throws ClassNotFoundException, SQLException, CZEPMySQLManagerException
    {
        //ds setup driver
        Class.forName( m_strMySQLDriver );
        
        //ds set login timeout (seconds)
        DriverManager.setLoginTimeout( m_iMySQLTimeoutMS/1000 );
        
        //ds establish connection
        m_cMySQLConnection = DriverManager.getConnection( m_strMySQLServerURL, m_strMySQLUsername, m_strMySQLPassword );      
        
        //ds check connection
        if( !m_cMySQLConnection.isValid( m_iMySQLTimeoutMS ) )
        {
            //ds escape
            throw new CZEPMySQLManagerException( "timeout (" + m_iMySQLTimeoutMS +  " ms) reached during connectivity test" );
        }
    }
    
    //ds check for emptyness
    public final boolean isEmpty( ) throws SQLException, CZEPMySQLManagerException
    {
        //ds max id is zero for empty table
        return ( 0 == _getMaxIDFromTable( "id_datapoint", "datapoints" ) );
    }
    
    //ds get pattern number
    public final int getNumberOfPatterns( final int p_iTagCutoffFrequency ) throws SQLException, CZEPMySQLManagerException
    {
        //ds table
        final String strTablePatterns = "patterns_" + Integer.toString( p_iTagCutoffFrequency );
        
        //ds max id is zero for empty table
        return _getMaxIDFromTable( "id_pattern", strTablePatterns );
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
            final PreparedStatement cStatementInsertDataPoint = m_cMySQLConnection.prepareStatement( "INSERT INTO `datapoints` " +
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
            cStatementInsertDataPoint.execute( );          
            
            //ds obtain the datapoint id
            final ResultSet cResultDataPoint = cStatementCheckDataPoint.executeQuery( );
            
            //ds this must work now
            if( !cResultDataPoint.next( ) ){ throw new CZEPMySQLManagerException( "could not insert DataPoint" ); }
            
            //ds get the id
            final int iID_DataPoint = cResultDataPoint.getInt( "id_datapoint" );
            
            //ds add the image file to the database
            final PreparedStatement cStatementInsertImage = m_cMySQLConnection.prepareStatement( "INSERT INTO `images` ( `id_datapoint`, `data_binary` ) VALUE ( ?, ? )" );
            cStatementInsertImage.setInt( 1, iID_DataPoint );
            cStatementInsertImage.setBlob( 2, new URL( p_cDataPoint.getURL( ).toString( ) ).openStream( ) );
            cStatementInsertImage.executeQuery( );
            
            //ds logging last tag id
            int iID_TagCurrent = _getMaxIDFromTable( "id_tag", "tags" );
            
            //ds then add the tags to the tags table
            for( CTag cTag : p_cDataPoint.getTags( ) )
            {
            	//ds for each tag check if we already have an entry in the tags map
                final PreparedStatement cStatementCheckTags = m_cMySQLConnection.prepareStatement( "SELECT * from `tags` WHERE `value` = ( ? ) LIMIT 1" );
                cStatementCheckTags.setString( 1, cTag.getValue( ) );
                
                //ds get the result
                final ResultSet cResultTag = cStatementCheckTags.executeQuery( );
                
                //ds if there is no entry yet
                if( !cResultTag.next( ) )
                {
                	//ds add the tag to the tags table
                    final PreparedStatement cStatementInsertTag = m_cMySQLConnection.prepareStatement( "INSERT INTO `tags` ( `value`, `frequency` ) VALUE ( ?, ? )" );
                    cStatementInsertTag.setString( 1, cTag.getValue( ) );
                    cStatementInsertTag.setInt( 2, 1 );
                    cStatementInsertTag.execute( );
                    
                    //ds increment tag counter
                    ++iID_TagCurrent;
                }
                else
                {
                	//ds increment the frequency counter - first obtain it
                    final int iCounter = cResultTag.getInt( "frequency" );
                    
                    //ds insert the incremented counter
                    final PreparedStatement cStatementInsertTag = m_cMySQLConnection.prepareStatement( "UPDATE `tags` SET `frequency` = ( ? ) WHERE `value` = ( ? )" );
                    cStatementInsertTag.setInt( 1, iCounter+1 );
                    cStatementInsertTag.setString( 2, cTag.getValue( ) );
                    cStatementInsertTag.execute( );                    
                }
                
                //ds now we have to create the linkage between tags and datapoints over the patterns table - execute the query for the tag again
                final ResultSet cResultTagID = cStatementCheckTags.executeQuery( );
                
                //ds this must work now
                if( !cResultTagID.next( ) ){ throw new CZEPMySQLManagerException( "from Tag: could not establish datapoint - tag linkage" ); }
                
                //ds obtain the tag id
                final int iID_Tag = cResultTagID.getInt( "id_tag" );
                
                //ds establish the linkage between id_datapoint and id_tag
                final PreparedStatement cStatementInsertLinkage = m_cMySQLConnection.prepareStatement( "INSERT INTO `mappings` ( `id_datapoint`, `id_tag` ) VALUE ( ?, ? )" );
                cStatementInsertLinkage.setInt( 1, iID_DataPoint );
                cStatementInsertLinkage.setInt( 2, iID_Tag );
                cStatementInsertLinkage.execute( );
            }
            
            //ds get current total tag number (simply the size of the mappings table)
            final int iCurrentTotalTags = _getMaxIDFromTable( "id_mapping", "mappings" );
            
            //ds open writer - locally because we want the data written in case the process dies unexpectedly
            FileWriter writer = new FileWriter( m_strLogFileTagGrowth, true );
            
            //ds append entry
            writer.append( iID_DataPoint + "," + iID_TagCurrent + "," + iCurrentTotalTags + "\n" );
            
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
    
    /*ds fetches a set of datapoints
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
    }*/

    /*ds fetches a set of patterns
    public final Vector< CPattern > getPatternsByIDRange( final int p_iIDStart, final int p_iIDEnd, final int p_iTagCutoffFrequency ) throws SQLException, CZEPMySQLManagerException
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(getPatternsByIDRange) Received fetch request for: [" + p_iIDStart + "," + p_iIDEnd + "]" );
        
      //ds table
        final String strTablePatterns = "patterns_" + Integer.toString( p_iTagCutoffFrequency );
        
        //ds get max id in database
        final int iIDMaximum = _getMaxIDFromTable( "id_pattern", strTablePatterns );
        
        //ds check if we want to access to much
        if( p_iIDEnd > iIDMaximum )
        {
            //ds escape
            throw new CZEPMySQLManagerException( "index: " + p_iIDEnd + " is out of range: " + iIDMaximum );            
        }
            
        //ds target number of elements to fetch
        final int iFetchSize = p_iIDEnd-p_iIDStart+1;
            
        //ds vector to fill
        final Vector< CPattern > vecPatterns = new Vector< CPattern >( iFetchSize );
        
        //ds query for the id
        final PreparedStatement cRetrievePattern = m_cMySQLConnection.prepareStatement( "SELECT * FROM `" + strTablePatterns + "` WHERE `id_pattern` >= ( ? )" );
        cRetrievePattern.setInt( 1, p_iIDStart );
        
        //ds get the result
        final ResultSet cResultSetPattern = cRetrievePattern.executeQuery( );
        
        //ds as long as we have remaining data and do not exceed the fetch size
        while( cResultSetPattern.next( ) && iFetchSize > vecPatterns.size( ) )
        {
            //ds add the datapoint to the vector
            vecPatterns.add( _getPatternFromResultSet( cResultSetPattern, p_iTagCutoffFrequency ) );        
        }
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(getDataPointsByIDRange) Number of points fetched: " + vecPatterns.size( ) );
        
        //ds return the elements
        return vecPatterns;
    }*/
    
    //ds fetches a single pattern
    public final CPattern getPatternByID( final int p_iID, final int p_iTagCutoffFrequency ) throws SQLException, CZEPMySQLManagerException
    {
        //ds table
        final String strTablePatterns = "patterns_" + Integer.toString( p_iTagCutoffFrequency );
        
        //ds query for the id
        final PreparedStatement cRetrievePattern = m_cMySQLConnection.prepareStatement( "SELECT * FROM `" + strTablePatterns + "` WHERE `id_pattern` = ( ? ) LIMIT 1" );
        cRetrievePattern.setInt( 1, p_iID );
        
        //ds get the result
        final ResultSet cResultSetPattern = cRetrievePattern.executeQuery( );
        
        //ds if the call succeeded
        if( cResultSetPattern.next( ) )
        {
            //ds add the datapoint to the vector
            return _getPatternFromResultSet( cResultSetPattern, p_iTagCutoffFrequency );        
        }
        else
        {
            throw new CZEPMySQLManagerException( "could not load pattern from MySQL database - ID: " + p_iID );
        }
    }
    
    //ds regular image access
    public final BufferedImage getBufferedImage( final CPattern p_cPattern ) throws CZEPMySQLManagerException
    {
        try
        {
            //ds grab the image row
            final PreparedStatement cRetrieveImage = m_cMySQLConnection.prepareStatement( "SELECT * FROM `images` WHERE `id_datapoint` = ( ? ) LIMIT 1" );
            cRetrieveImage.setInt( 1, p_cPattern.getID( ) );

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
                throw new CZEPMySQLManagerException( "could not load image from MySQL database - ID: " + p_cPattern.getID( ) );
            }
        }
        catch( SQLException e )
        {
            throw new CZEPMySQLManagerException( "SQLException: " + e.getMessage( ) + " could not load image from MySQL database - ID: " + p_cPattern.getID( ) );
        }
        catch( IOException e )
        {
            throw new CZEPMySQLManagerException( "IOException: " + e.getMessage( ) + " could not load image from MySQL database - ID: " + p_cPattern.getID( ) );
        }
    }

    //ds gif image access
    public final ImageIcon getImageIcon( final CPattern p_cPattern ) throws CZEPMySQLManagerException
    {
        try
        {
            //ds grab the image row
            final PreparedStatement cRetrieveImage = m_cMySQLConnection.prepareStatement( "SELECT * FROM `images` WHERE `id_datapoint` = ( ? ) LIMIT 1" );
            cRetrieveImage.setInt( 1, p_cPattern.getID( ) );
        
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
                throw new CZEPMySQLManagerException( "could not load image from MySQL database - ID: " + p_cPattern.getID( ) );
            }
        }
        catch( SQLException e )
        {
            throw new CZEPMySQLManagerException( "SQLException: " + e.getMessage( ) + " could not load image from MySQL database - ID: " + p_cPattern.getID( ) );    		
        }
    }
    
    /*ds access function: multiple datapoints
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
    }*/
    
    //ds getter for the probability map
    public final Map< Integer, Double > getProbabilityMap( final int p_iTagCutoffFrequency ) throws CZEPMySQLManagerException, SQLException
    {
        //ds table
        final String strTableProbabilities = "probabilities_" + Integer.toString( p_iTagCutoffFrequency );
        
        //ds get number of entries
        final int iMapSize = _getMaxIDFromTable( "id_probability", strTableProbabilities );
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(getProbabilityMap) received fetch request - table size: " + iMapSize );
        
        //ds map to fill
        Map< Integer, Double > mapProbabilities = new HashMap< Integer, Double >( iMapSize );
        
        //ds import the information from the db
        final PreparedStatement cRetrieveProbability = m_cMySQLConnection.prepareStatement( "SELECT * FROM `" + strTableProbabilities + "` WHERE `id_probability` >= 1" );
        
        //ds get the result
        final ResultSet cResultSetProbability = cRetrieveProbability.executeQuery( );
        
        //ds as long as we have remaining data
        while( cResultSetProbability.next( ) )
        {
            //ds add to the map
            mapProbabilities.put( cResultSetProbability.getInt( "id_feature" ), cResultSetProbability.getDouble( "value" ) );
        }
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(getProbabilityMap) download complete fetched elements: " + mapProbabilities.size( ) );
        
        //ds return the map
        return mapProbabilities;
    }
    
    //ds create feature table
    public final void createPatternsTable( final int p_iTagCutoffFrequency, final int p_iID_DataPointStart ) throws SQLException, IOException
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(createFeatureTable) creating the final feature table starting at ID: " + p_iID_DataPointStart );
        
        //ds get input as string
        final String strMinimumTagFrequency = Integer.toString( p_iTagCutoffFrequency );
        
        //ds table names
        final String strTablePatterns      = "patterns_" + strMinimumTagFrequency;
        //final String strTableMappings      = "mappings_" + strMinimumTagFrequency;
        
        //ds query for the first id - select all datapoints
        final PreparedStatement cRetrieveDataPoint = m_cMySQLConnection.prepareStatement( "SELECT * FROM `datapoints` WHERE `id_datapoint` >= ( ? )" );
        cRetrieveDataPoint.setInt( 1, p_iID_DataPointStart );
        
        //ds get the result
        final ResultSet cResultSetDataPoint = cRetrieveDataPoint.executeQuery( );
        
        //ds discard counter
        int iNumberOfDiscardedPoints = 0;
        
        //ds as long as we have remaining data
        while( cResultSetDataPoint.next( ) )
        {        
            //ds get the datapoint
            final CDataPoint cDataPoint = _getDataPointFromResultSet( cResultSetDataPoint );
            
            //ds tag ids to use
            final Vector< Integer > vecTagIDs = new Vector< Integer >( 0 );
            
            //ds we now have to check the tag frequencies
            for( CTag cTag: cDataPoint.getTags( ) )
            {
                //ds if the frequency is above the threshold
                if( p_iTagCutoffFrequency < cTag.getFrequency( ) )
                {
                    //ds add the id
                    vecTagIDs.add( cTag.getID( ) );
                }
            }
            
            //ds check if there was no tag with minimum frequency fullfiled
            if( vecTagIDs.isEmpty( ) )
            {
                //ds count
                ++iNumberOfDiscardedPoints;
                
                //ds check next datapoint
                continue;
            }
            
            //ds get insertion query for the feature table
            final PreparedStatement cStatementInsertFeaturePoint = m_cMySQLConnection.prepareStatement( "INSERT INTO `" + strTablePatterns + "` ( `id_datapoint`, `title`, `animated`, `photo`, `text`, `liked`, `hot` ) VALUE ( ?, ?, ?, ?, ?, ?, ? )" );
            
            //ds determine values
            final int iID_DataPoint   = cDataPoint.getID( );
            final boolean bIsAnimated = cDataPoint.getType( ).matches( "gif" );
            final boolean bIsPhoto    = cDataPoint.isPhoto( );
            final boolean isText      = cDataPoint.getTextAmount( ) > m_dMinimumText;
            final boolean isLiked     = ( double )( cDataPoint.getLikes( ) )/( cDataPoint.getDislikes( )+1  ) > m_dMinimumLikeDislikeRatio;
            final boolean isHot       = ( double )( cDataPoint.getLikes( )+cDataPoint.getDislikes( ) )/( cDataPoint.getCountComments( )+1 ) < m_dMaximumVotesCommentsRatio;
            
            //ds set params
            cStatementInsertFeaturePoint.setInt( 1, iID_DataPoint );  
            cStatementInsertFeaturePoint.setString( 2, cDataPoint.getTitle( ) );  
            cStatementInsertFeaturePoint.setBoolean( 3, bIsAnimated );
            cStatementInsertFeaturePoint.setBoolean( 4, bIsPhoto );
            cStatementInsertFeaturePoint.setBoolean( 5, isText );
            cStatementInsertFeaturePoint.setBoolean( 6, isLiked );
            cStatementInsertFeaturePoint.setBoolean( 7, isHot );

            //ds execute
            cStatementInsertFeaturePoint.execute( );
            
            /*ds loop over valid tags
            for( final int iID_Tag: vecTagIDs )
            {
                //ds insertion into mappings table
                final PreparedStatement cStatementInsertLinkage = m_cMySQLConnection.prepareStatement( "INSERT INTO `" + strTableMappings + "` ( `id_datapoint`, `id_tag` ) VALUE ( ?, ? )" );
                cStatementInsertLinkage.setInt( 1, iID_DataPoint );
                cStatementInsertLinkage.setInt( 2, iID_Tag );
                cStatementInsertLinkage.execute( );        
            }*/
        }
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(createFeatureTable) feature table creation successful - discarded datapoints: " + iNumberOfDiscardedPoints );
    }
    
    //ds compute probabilities
    public final void computeProbabilities( final int p_iTagCutoffFrequency ) throws SQLException, CZEPMySQLManagerException
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(computeProbabilities) computing probabilities for cutoff frequency: " +  p_iTagCutoffFrequency );
        
        //ds get input as string
        final String strMinimumTagFrequency = Integer.toString( p_iTagCutoffFrequency );
        
        //ds table names
        final String strTableProbabilities = "probabilities_" + strMinimumTagFrequency;
        final String strTablePatterns      = "patterns_" + strMinimumTagFrequency;
        
        //ds get total values
        final int iTotalNumberOfPatterns = _getMaxIDFromTable( "id_datapoint", strTablePatterns );
        
        //ds simple feature counters
        int iCounterAnimated = 0;
        int iCounterPhoto    = 0;
        int iCounterText     = 0;
        int iCounterLiked    = 0;
        int iCounterHot      = 0;
        
        //ds we have to loop through the whole datapoint table
        final PreparedStatement cRetrievePattern = m_cMySQLConnection.prepareStatement( "SELECT * FROM `" + strTablePatterns + "` WHERE `id_datapoint` >= 1" );     
        
        //ds get the result
        final ResultSet cResultSetDataPoint = cRetrievePattern.executeQuery( );
        
        //ds as long as we have remaining data
        while( cResultSetDataPoint.next( ) )
        {
            //ds extract the data separately (for readability)
            final boolean bIsAnimated = cResultSetDataPoint.getBoolean( "animated" );
            final boolean bIsPhoto    = cResultSetDataPoint.getBoolean( "photo" );
            final boolean bIsText     = cResultSetDataPoint.getBoolean( "text" );
            final boolean bIsLiked    = cResultSetDataPoint.getBoolean( "liked" );
            final boolean bIsHot      = cResultSetDataPoint.getBoolean( "hot" );
            
            //ds set the counters if they match
            if( bIsAnimated ){ ++iCounterAnimated; }
            if( bIsPhoto ){ ++iCounterPhoto; }
            if( bIsText ){ ++iCounterText; }
            if( bIsLiked ){ ++iCounterLiked; }
            if( bIsHot ){ ++iCounterHot; }
        }
        
        //ds compute final probabilities
        final double dProbabilityAnimated = ( double )iCounterAnimated/iTotalNumberOfPatterns;
        final double dProbabilityPhoto    = ( double )iCounterPhoto/iTotalNumberOfPatterns;
        final double dProbabilityText     = ( double )iCounterText/iTotalNumberOfPatterns;
        final double dProbabilityLiked    = ( double )iCounterLiked/iTotalNumberOfPatterns;
        final double dProbabilityHot      = ( double )iCounterHot/iTotalNumberOfPatterns;
        
        //ds write the values to the database
        final PreparedStatement cStatementInsertValues = m_cMySQLConnection.prepareStatement( "INSERT INTO `" + strTableProbabilities + "` ( `id_feature`, `value`, `description` ) " +
        		                                                                              "VALUES ( ?, ?, ? ), ( ?, ?, ? ), ( ?, ?, ? ), ( ?, ?, ? ), ( ?, ?, ? )" );
        cStatementInsertValues.setInt( 1, -1 );
        cStatementInsertValues.setDouble( 2, dProbabilityAnimated );
        cStatementInsertValues.setString( 3, "animated" );
        cStatementInsertValues.setInt( 4, -2 );
        cStatementInsertValues.setDouble( 5, dProbabilityPhoto );
        cStatementInsertValues.setString( 6, "photo" );
        cStatementInsertValues.setInt( 7, -3 );
        cStatementInsertValues.setDouble( 8, dProbabilityText );
        cStatementInsertValues.setString( 9, "text" );
        cStatementInsertValues.setInt( 10, -4 );
        cStatementInsertValues.setDouble( 11, dProbabilityLiked );
        cStatementInsertValues.setString( 12, "liked" );
        cStatementInsertValues.setInt( 13, -5 );
        cStatementInsertValues.setDouble( 14, dProbabilityHot );
        cStatementInsertValues.setString( 15, "hot" );
        
        //ds commit
        cStatementInsertValues.execute( );
        
        //ds vector with tags
        Vector< CTag > vecTags = new Vector< CTag >( 0 ); 
        
        //ds total number of occurrences
        int iTotalNumberOfTags = 0;
        
        //ds retrieve all tags fullfilling the cutoff
        final PreparedStatement cRetrieveTag = m_cMySQLConnection.prepareStatement( "SELECT * FROM `tags` WHERE `frequency` > ( ? )" );
        cRetrieveTag.setInt( 1, p_iTagCutoffFrequency );
        
        //ds get the result
        final ResultSet cResultSetTag = cRetrieveTag.executeQuery( );
        
        //ds as long as we have remaining data
        while( cResultSetTag.next( ) )
        {
            //ds get frequency information
            final int iFrequency = cResultSetTag.getInt( "frequency" );
            
            //ds add the tag
            vecTags.add( new CTag( cResultSetTag.getInt( "id_tag" ), cResultSetTag.getString( "value" ), iFrequency ) );
            
            //ds sum up total
            iTotalNumberOfTags += iFrequency;
        }
        
        //ds now create probability entries
        for( CTag cTag: vecTags )
        {
            //ds derive probability from entry
            final double dProbabilityTag = ( double )cTag.getFrequency( )/iTotalNumberOfTags;
            
            //ds insert tag probability
            final PreparedStatement cStatementInsertTag = m_cMySQLConnection.prepareStatement( "INSERT INTO `" + strTableProbabilities + "` ( `id_feature`, `value`, `description` ) VALUES ( ?, ?, ? )" );
            cStatementInsertTag.setInt( 1, cTag.getID( ) );
            cStatementInsertTag.setDouble( 2, dProbabilityTag );
            cStatementInsertTag.setString( 3, cTag.getValue( ) );
            
            //ds commit
            cStatementInsertTag.execute( );
        }
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(computeProbabilities) computation complete" );
    }
    
    //ds log to master database
    public final void logMaster( final String p_strUsername, final String p_strText ) throws SQLException
    {
        //ds simple insertion
        final PreparedStatement cStatementInsertText = m_cMySQLConnection.prepareStatement( "INSERT INTO `log_master` ( `username`, `text` ) VALUE ( ?, ? )" );
        cStatementInsertText.setString( 1, p_strUsername );
        cStatementInsertText.setString( 2, p_strText );
        cStatementInsertText.execute( );
    }
    
    //ds set active user
    public final void setActiveUser( final String p_strUsername ) throws SQLException, CZEPMySQLManagerException
    {
        //ds check if available
        if( isUserAvailable( p_strUsername ) )
        {
            //ds simple insertion
            final PreparedStatement cStatementAddUser = m_cMySQLConnection.prepareStatement( "INSERT INTO `active_users` ( `username` ) VALUE ( ? )" );
            cStatementAddUser.setString( 1, p_strUsername );
            cStatementAddUser.execute( );
        }
        else
        {
            //ds escape
            throw new CZEPMySQLManagerException( "could not set username to active users" );         
        }
    }
    
    //ds remove active user
    public final void removeActiveUser( final String p_strUsername ) throws SQLException, CZEPMySQLManagerException
    {
        //ds check if not available (user has to be active)
        if( !isUserAvailable( p_strUsername ) )
        {
            //ds simple insertion
            final PreparedStatement cStatementRemoveUser = m_cMySQLConnection.prepareStatement( "DELETE FROM `active_users` WHERE ( `username` ) = ( ? )" );
            cStatementRemoveUser.setString( 1, p_strUsername );
            cStatementRemoveUser.execute( );
        }
        else
        {
            //ds escape
            throw new CZEPMySQLManagerException( "could not remove user from active list - no matching entry present" );         
        }
    }
    
    //ds check for active user
    public final boolean isUserAvailable( final String p_strUsername ) throws SQLException
    {
        //ds query for the username
        final PreparedStatement cRetrieveUser = m_cMySQLConnection.prepareStatement( "SELECT * FROM `active_users` WHERE `username` = ( ? ) LIMIT 1" );
        cRetrieveUser.setString( 1, p_strUsername );
        
        //ds get the result
        final ResultSet cResultSetUser = cRetrieveUser.executeQuery( );
        
        //ds if we got a result the user is active - we cannot take this name
        if( cResultSetUser.next( ) )
        {
            //ds blocked
            return false;
        }
        else
        {
            //ds available
            return true;
        }
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
    
    //ds helper for data element retrieval
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
        final Vector< CTag > vecTags = new Vector< CTag >( );
        
        //ds now retrieve the tag information
        final PreparedStatement cRetrieveMapping = m_cMySQLConnection.prepareStatement( "SELECT * FROM `mappings` WHERE `id_datapoint` = ( ? )" );
        cRetrieveMapping.setInt( 1, iID_DataPoint );
        
        //ds get the mapping
        final ResultSet cResultSetMapping = cRetrieveMapping.executeQuery( );
        
        //ds for all the mappings
        while( cResultSetMapping.next( ) )
        {
            //ds get tag id
            final int iID_Tag = cResultSetMapping.getInt( "id_tag" );
       
            //ds get actual tag text
            final PreparedStatement cRetrieveTag = m_cMySQLConnection.prepareStatement( "SELECT * FROM `tags` WHERE `id_tag` = ( ? ) LIMIT 1" );
            cRetrieveTag.setInt( 1, iID_Tag );
            
            //ds get the tag
            final ResultSet cResultSetTag = cRetrieveTag.executeQuery( );
            
            //ds if exists
            if( cResultSetTag.next( ) )
            {
                //ds add the current tag
                vecTags.add( new CTag( iID_Tag, cResultSetTag.getString( "value" ), cResultSetTag.getInt( "frequency" ) ) );
            }
        }
        
        //ds return the datapoint
        return new CDataPoint( iID_DataPoint, new URL( strURL ), strTitle, strType, iLikes, iDislikes, iCountComments, iCountTags, bIsPhoto, dTextAmount, vecTags );
    }
    
    //ds helper for pattern retrieval
    private final CPattern _getPatternFromResultSet( final ResultSet p_cResultSetDataPoint, final int p_iTagCutoffFrequency ) throws SQLException, CZEPMySQLManagerException
    {
        //ds get the ID
        final int iID_DataPoint = p_cResultSetDataPoint.getInt( "id_datapoint" );
        
        //ds extract the data separately (for readability)
        final String strTitle     = p_cResultSetDataPoint.getString( "title" );
        final boolean bIsAnimated = p_cResultSetDataPoint.getBoolean( "animated" );
        final boolean bIsPhoto    = p_cResultSetDataPoint.getBoolean( "photo" );
        final boolean bIsText     = p_cResultSetDataPoint.getBoolean( "text" );
        final boolean bIsLiked    = p_cResultSetDataPoint.getBoolean( "liked" );
        final boolean bIsHot      = p_cResultSetDataPoint.getBoolean( "hot" );
        
        //ds tag vector to be filled
        final Vector< CTag > vecTags = new Vector< CTag >( );
        
        //ds now retrieve the tag information
        final PreparedStatement cRetrieveMapping = m_cMySQLConnection.prepareStatement( "SELECT * FROM `mappings` WHERE `id_datapoint` = ( ? )" );
        cRetrieveMapping.setInt( 1, iID_DataPoint );
        
        //ds get the mapping
        final ResultSet cResultSetMapping = cRetrieveMapping.executeQuery( );
        
        //ds for all the mappings
        while( cResultSetMapping.next( ) )
        {
            //ds get tag id
            final int iID_Tag = cResultSetMapping.getInt( "id_tag" );
       
            //ds get actual tag text
            final PreparedStatement cRetrieveTag = m_cMySQLConnection.prepareStatement( "SELECT * FROM `tags` WHERE `id_tag` = ( ? ) AND `frequency` > ( ? ) LIMIT 1" );
            cRetrieveTag.setInt( 1, iID_Tag );
            cRetrieveTag.setInt( 2, p_iTagCutoffFrequency );
            
            //ds get the tag
            final ResultSet cResultSetTag = cRetrieveTag.executeQuery( );
            
            //ds if exists
            if( cResultSetTag.next( ) )
            {
                //ds add the current tag
                vecTags.add( new CTag( iID_Tag, cResultSetTag.getString( "value" ), cResultSetTag.getInt( "frequency" ) ) );
            }
        }
        
        //ds check if we got an error - no tags found for the current cutoff
        if( vecTags.isEmpty( ) )
        {
            //ds escape
            throw new CZEPMySQLManagerException( "invalid datapoint: " + iID_DataPoint + " - no tag with minimum frequency: " + p_iTagCutoffFrequency );                  
        }
        
        //ds return the datapoint
        return new CPattern( iID_DataPoint, strTitle, bIsAnimated, bIsPhoto, bIsText, bIsLiked, bIsHot, vecTags );
    }
}
