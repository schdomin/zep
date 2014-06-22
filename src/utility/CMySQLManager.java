package utility;

import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

//ds custom imports
import utility.CTag;
import exceptions.CZEPConfigurationException;
import exceptions.CZEPMySQLManagerException;

public final class CMySQLManager
{
    //ds soft attributes
    //private final String m_strMySQLDriver;
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
    public CMySQLManager( final String p_strMySQLServerURL, final String p_strMySQLUsername, final String p_strMySQLPassword )
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(CMySQLManager) Instance allocated" );
        
        //ds fix the logfile name
        m_strLogFileTagGrowth += CLogger.getFileStamp( ) + ".csv";
        
        //ds set values
        //m_strMySQLDriver    = p_strMySQLDriver;
        m_strMySQLServerURL = p_strMySQLServerURL;
        m_strMySQLUsername  = p_strMySQLUsername;
        m_strMySQLPassword  = p_strMySQLPassword;
    }
    
    //ds standalone
    public static void main( String[] args )
    {        
        //ds default configuration parameters: mysql
        String strMySQLServerURL = "";
        String strMySQLUsername  = "";
        String strMySQLPassword  = "";
        final int iTagCutoffFrequency = 10;
        
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
        
        //ds allocate the MySQL manager
        final CMySQLManager cMySQLManager = new CMySQLManager( strMySQLServerURL, strMySQLUsername, strMySQLPassword );
        
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
            cMySQLManager.createPatternsAndTagsTable( iTagCutoffFrequency );
        }
        catch( Exception e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(main) Exception: " + e.getMessage( ) + " - failed" );           
        }
        try
        {
            //ds compute probability table
            cMySQLManager.computeProbabilities( iTagCutoffFrequency );
        }
        catch( Exception e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(main) Exception: " + e.getMessage( ) + " - failed" ); 
        }
        
        //ds exit and close jvm
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(main) terminated" ); 
        System.exit( 0 );
        return;
    }
    
    //ds launch - establishes connection
    public final void launch( ) throws ClassNotFoundException, SQLException, CZEPMySQLManagerException
    {
        //ds set login timeout (seconds)
        DriverManager.setLoginTimeout( m_iMySQLTimeoutMS/1000 );
        
        //ds establish connection
        m_cMySQLConnection = DriverManager.getConnection( m_strMySQLServerURL, m_strMySQLUsername, m_strMySQLPassword );
        
        //ds set networking timeout
        m_cMySQLConnection.setNetworkTimeout( Executors.newFixedThreadPool( 1 ), m_iMySQLTimeoutMS );
        
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
        final String strTablePatterns = "cutoff_" + Integer.toString( p_iTagCutoffFrequency ) + "_patterns";
        
        //ds max id is zero for empty table
        return _getMaxIDFromTable( "id_pattern", strTablePatterns );
    }
    
    //ds get tags number
    public final Map< Integer, Integer > getNumbersOfTag( final int p_iTagCutoffFrequency ) throws SQLException, CZEPMySQLManagerException
    {
        //ds table
        final String strTableTags = "cutoff_" + Integer.toString( p_iTagCutoffFrequency ) + "_tags";
        
        //ds retrieve all tags fulfilling the cutoff
        final PreparedStatement cRetrieveTag = m_cMySQLConnection.prepareStatement( "SELECT * FROM `" + strTableTags + "`" );
        cRetrieveTag.setQueryTimeout( m_iMySQLTimeoutMS );
        
        //ds get the result
        final ResultSet cResultSetTag = cRetrieveTag.executeQuery( );
        
        //ds frequency map
        Map< Integer, Integer > mapNumbersOfTag = new HashMap< Integer, Integer >( 0 );
        
        //ds evaluate all results
        while( cResultSetTag.next( ) )
        {
            //ds update total number
            mapNumbersOfTag.put( cResultSetTag.getInt( "id_tag" ), cResultSetTag.getInt( "frequency" ) );
        }
        
        //ds return the map
        return mapNumbersOfTag;
    }
    
    //ds insert datapoint
    public final void insertDataPoint( final CDataPoint p_cDataPoint ) throws SQLException, CZEPMySQLManagerException, MalformedURLException, IOException
    {
        //ds first check if we already have an entry for this image (no double URLs allowed)
        final PreparedStatement cStatementCheckDataPoint = m_cMySQLConnection.prepareStatement( "SELECT `id_datapoint` from `datapoints` WHERE `url` = ( ? ) LIMIT 1" );
        cStatementCheckDataPoint.setString( 1, p_cDataPoint.getURL( ).toString( ) );
        cStatementCheckDataPoint.setQueryTimeout( m_iMySQLTimeoutMS );
        
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
            cStatementInsertDataPoint.setQueryTimeout( m_iMySQLTimeoutMS );
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
            cStatementInsertImage.setQueryTimeout( m_iMySQLTimeoutMS );
            cStatementInsertImage.execute( );
            
            //ds logging last tag id
            int iID_TagCurrent = _getMaxIDFromTable( "id_tag", "tags" );
            
            //ds then add the tags to the tags table
            for( CTag cTag : p_cDataPoint.getTags( ) )
            {
            	//ds for each tag check if we already have an entry in the tags map
                final PreparedStatement cStatementCheckTags = m_cMySQLConnection.prepareStatement( "SELECT * from `tags` WHERE `value` = ( ? ) LIMIT 1" );
                cStatementCheckTags.setString( 1, cTag.getValue( ) );
                cStatementCheckTags.setQueryTimeout( m_iMySQLTimeoutMS );
                
                //ds get the result
                final ResultSet cResultTag = cStatementCheckTags.executeQuery( );
                
                //ds if there is no entry yet
                if( !cResultTag.next( ) )
                {
                	//ds add the tag to the tags table
                    final PreparedStatement cStatementInsertTag = m_cMySQLConnection.prepareStatement( "INSERT INTO `tags` ( `value`, `frequency` ) VALUE ( ?, ? )" );
                    cStatementInsertTag.setString( 1, cTag.getValue( ) );
                    cStatementInsertTag.setInt( 2, 1 );
                    cStatementInsertTag.setQueryTimeout( m_iMySQLTimeoutMS );
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
                    cStatementInsertTag.setQueryTimeout( m_iMySQLTimeoutMS );
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
                cStatementInsertLinkage.setQueryTimeout( m_iMySQLTimeoutMS );
                cStatementInsertLinkage.execute( );
            }
            
            //ds get current total tag number (simply the size of the mappings table)
            final int iCurrentTotalTags = _getMaxIDFromTable( "id_mapping", "mappings" );
            
            //ds open writer - locally because we want the data written in case the process dies unexpectedly
            FileWriter cWriter = new FileWriter( m_strLogFileTagGrowth, true );
            
            //ds append entry
            cWriter.append( iID_DataPoint + "," + iID_TagCurrent + "," + iCurrentTotalTags + "\n" );
            
            //ds close writer
            cWriter.close( );
        }
        else
        {
            throw new CZEPMySQLManagerException( "element already in database" );
        }
    }
    
    /*ds access function: single datapoint by id
    public final CDataPoint getDataPointByID( final int p_iID_DataPoint ) throws SQLException, MalformedURLException, CZEPMySQLManagerException
    {
        //ds query for the id
        final PreparedStatement cRetrieveDataPoint = m_cMySQLConnection.prepareStatement( "SELECT * FROM `datapoints` WHERE `id_datapoint` = ( ? ) LIMIT 1" );
        cRetrieveDataPoint.setInt( 1, p_iID_DataPoint );
        cRetrieveDataPoint.setQueryTimeout( m_iMySQLTimeoutMS );
        
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
    }*/
    
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
    
    /*ds fetches a single pattern
    public final CPattern getPatternByID( final int p_iID, final int p_iTagCutoffFrequency ) throws SQLException, CZEPMySQLManagerException
    {
        //ds table
        final String strTablePatterns = "cutoff_" + Integer.toString( p_iTagCutoffFrequency ) + "_patterns";
        
        //ds query for the id
        final PreparedStatement cRetrievePattern = m_cMySQLConnection.prepareStatement( "SELECT * FROM `" + strTablePatterns + "` WHERE `id_pattern` = ( ? ) LIMIT 1" );
        cRetrievePattern.setInt( 1, p_iID );
        cRetrievePattern.setQueryTimeout( m_iMySQLTimeoutMS );
        
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
    }*/
    
    //ds regular image access
    public final BufferedImage getBufferedImage( final CPattern p_cPattern ) throws CZEPMySQLManagerException
    {
        try
        {
            //ds grab the image row
            final PreparedStatement cRetrieveImage = m_cMySQLConnection.prepareStatement( "SELECT * FROM `images` WHERE `id_datapoint` = ( ? ) LIMIT 1" );
            cRetrieveImage.setInt( 1, p_cPattern.getID( ) );
            cRetrieveImage.setQueryTimeout( m_iMySQLTimeoutMS );

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
            cRetrieveImage.setQueryTimeout( m_iMySQLTimeoutMS );
        
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
    
    //ds binary stream
    public final InputStream getImageStream( final CPattern p_cPattern ) throws CZEPMySQLManagerException
    {
        try
        {
            //ds grab the image row
            final PreparedStatement cRetrieveImage = m_cMySQLConnection.prepareStatement( "SELECT * FROM `images` WHERE `id_datapoint` = ( ? ) LIMIT 1" );
            cRetrieveImage.setInt( 1, p_cPattern.getID( ) );
            cRetrieveImage.setQueryTimeout( m_iMySQLTimeoutMS );
        
            //ds execute the statement
            final ResultSet cResultSetImage = cRetrieveImage.executeQuery( );
        
            //ds if we got something
            if( cResultSetImage.next( ) )
            {
                //ds get the blob from mysql
                final Blob cBlob =  cResultSetImage.getBlob( "data_binary" );
        
                //ds return it
                return cBlob.getBinaryStream( );
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
    
    //ds access function for all patterns
    public final Vector< CPattern > getDataset( final int p_iTagCutoffFrequency ) throws SQLException, CZEPMySQLManagerException
    {
        //ds number of patterns to fetch
        final int iTotalNumberOfPatterns = getNumberOfPatterns( p_iTagCutoffFrequency );
        
        //ds tables
        final String strTagCutoff     = Integer.toString( p_iTagCutoffFrequency );
        final String strTablePatterns = "cutoff_" + strTagCutoff + "_patterns";
        final String strTableTags     = "cutoff_" + strTagCutoff + "_tags";
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(getDataset) received fetch request for [" + iTotalNumberOfPatterns + "] patterns" );
        
        //ds allocate a fresh vector
        final Vector< CPattern > vecDataset = new Vector< CPattern >( iTotalNumberOfPatterns );
        
        //ds query for the first pattern - implicit inner join
        final PreparedStatement cRetrievePatternAndTags = m_cMySQLConnection.prepareStatement
                                                         ( "SELECT * FROM " + strTablePatterns + " JOIN `mappings` ON " + strTablePatterns + ".`id_datapoint` = `mappings`.`id_datapoint` " +
                		                                                                          "JOIN " + strTableTags + " ON " + strTableTags + ".`id_tag` = `mappings`.`id_tag`" );
        
        //ds get the result
        final ResultSet cResultSetPattern = cRetrievePatternAndTags.executeQuery( );

        //ds previous pattern reference
        CPattern cPreviousPattern = null;
        
        //ds first pattern is done ahead to avoid more loop parameters and if clauses
        if( cResultSetPattern.next( ) )
        {
            //ds new pattern
            cPreviousPattern = _getPatternFromResultSet( cResultSetPattern );
            
            //ds add current tag
            cPreviousPattern.getTagsModifiable( ).add( new CTag( cResultSetPattern.getInt( "id_tag" ), cResultSetPattern.getString( "value" ), cResultSetPattern.getInt( "frequency" ) ) );
        }
        
        //ds as long as we have remaining data
        while( cResultSetPattern.next( ) )
        {
            //ds check if pattern id matches previous (we only have to add tags)
            if( cPreviousPattern.getID( ) == cResultSetPattern.getInt( "id_datapoint" ) )
            {
                //ds add current tag
                cPreviousPattern.getTagsModifiable( ).add( new CTag( cResultSetPattern.getInt( "id_tag" ), cResultSetPattern.getString( "value" ), cResultSetPattern.getInt( "frequency" ) ) );
            }
            else
            {
                //ds save the previous pattern (tag collection complete)
                vecDataset.add( cPreviousPattern );
                
                //ds new pattern
                cPreviousPattern = _getPatternFromResultSet( cResultSetPattern );
                
                //ds add current tag
                cPreviousPattern.getTagsModifiable( ).add( new CTag( cResultSetPattern.getInt( "id_tag" ), cResultSetPattern.getString( "value" ), cResultSetPattern.getInt( "frequency" ) ) );                
            }
        }
        
        //ds dont forget to add the last pattern
        vecDataset.add( cPreviousPattern );
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(getDataset) fetching complete" );
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(getDataset) added [" + vecDataset.size( ) + "/" + iTotalNumberOfPatterns + "] Patterns" );
        
        return vecDataset;
    }
    
    //ds getter for the probability map
    public final Map< Integer, Double > getProbabilityMap( final int p_iTagCutoffFrequency ) throws CZEPMySQLManagerException, SQLException
    {
        //ds table
        final String strTableProbabilities = "cutoff_" + Integer.toString( p_iTagCutoffFrequency ) + "_probabilities";
        
        //ds get number of entries
        final int iMapSize = _getMaxIDFromTable( "id_probability", strTableProbabilities );
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(getProbabilityMap) received fetch request - table size: " + iMapSize );
        
        //ds map to fill
        Map< Integer, Double > mapProbabilities = new HashMap< Integer, Double >( iMapSize );
        
        //ds import the information from the db
        final PreparedStatement cRetrieveProbability = m_cMySQLConnection.prepareStatement( "SELECT * FROM `" + strTableProbabilities + "` WHERE `id_probability` >= 1" );
        cRetrieveProbability.setQueryTimeout( m_iMySQLTimeoutMS );
        
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
    
    //ds get all tag incidies
    public final Vector< Integer > getTagIDs( final int p_iTagCutoffFrequency ) throws SQLException
    {
        //ds table
        final String strTableTags = "cutoff_" + Integer.toString( p_iTagCutoffFrequency ) + "_tags";
        
        //ds retrieve all tags fulfilling the cutoff
        final PreparedStatement cRetrieveTag = m_cMySQLConnection.prepareStatement( "SELECT * FROM `" + strTableTags + "`" );
        cRetrieveTag.setQueryTimeout( m_iMySQLTimeoutMS );
        
        //ds get the result
        final ResultSet cResultSetTag = cRetrieveTag.executeQuery( );
        
        //ds indices vector
        Vector< Integer > vecIDs = new Vector< Integer >( 0 );
        
        //ds as long as we have remaining data
        while( cResultSetTag.next( ) )
        {     
            //ds add the element
            vecIDs.add( cResultSetTag.getInt( "id_tag" ) );
        }
        
        //ds return all ids
        return vecIDs;
    }
    
    //ds create feature table
    public final void createPatternsAndTagsTable( final int p_iTagCutoffFrequency ) throws SQLException, IOException
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(createFeatureTable) creating the final feature table for cutoff frequency: " +  p_iTagCutoffFrequency );
        
        //ds table names
        final String strTagCutoff     = Integer.toString( p_iTagCutoffFrequency );
        final String strTablePatterns = "cutoff_" + strTagCutoff + "_patterns";
        final String strTableTags     = "cutoff_" + Integer.toString( p_iTagCutoffFrequency ) + "_tags";
        
      //ds query for the first datapoint - implicit inner join
        final PreparedStatement cRetrieveDataPointsAndTags = m_cMySQLConnection.prepareStatement
                                                             ( "SELECT * FROM `datapoints` JOIN `mappings` ON `datapoints`.`id_datapoint` = `mappings`.`id_datapoint` " +
                                                                                          "JOIN `tags` ON `tags`.`id_tag` = `mappings`.`id_tag`" );
        
        //ds get the result
        final ResultSet cResultSetDataPoint = cRetrieveDataPointsAndTags.executeQuery( );

        //ds previous datapoint reference
        CDataPoint cPreviousDataPoint = null;
        
        //ds discard counter
        int iNumberOfDiscardedPoints = 0;
        
        //ds first datapoint is done ahead to avoid more loop parameters and if clauses
        if( cResultSetDataPoint.next( ) )
        {
            //ds new datapoint (without tags)
            cPreviousDataPoint = _getDataPointFromResultSet( cResultSetDataPoint );
            
            //ds add current tag
            cPreviousDataPoint.getTagsModifiable( ).add( new CTag( cResultSetDataPoint.getInt( "id_tag" ), cResultSetDataPoint.getString( "value" ), cResultSetDataPoint.getInt( "frequency" ) ) );
        }
        
        //ds as long as we have remaining data
        while( cResultSetDataPoint.next( ) )
        {
            //ds check if pattern id matches previous (we only have to add tags)
            if( cPreviousDataPoint.getID( ) == cResultSetDataPoint.getInt( "id_datapoint" ) )
            {
                //ds add current tag
                cPreviousDataPoint.getTagsModifiable( ).add( new CTag( cResultSetDataPoint.getInt( "id_tag" ), cResultSetDataPoint.getString( "value" ), cResultSetDataPoint.getInt( "frequency" ) ) );
            }
            else
            {
                //ds no analyzation for gifs TODO GIFS
                if( cPreviousDataPoint.getType( ).equals( "gif" ) )
                {
                    ++iNumberOfDiscardedPoints;
                }
                else
                {
                    //ds check the previous datapoint (tag collection complete) - minimum 1 tag has to reach cutoff frequency
                    boolean bIsFrequencyAboveCutoff = false;
                    
                    //ds we now have to check the tag frequencies
                    for( CTag cTag: cPreviousDataPoint.getTags( ) )
                    {
                        //ds if the frequency is above the threshold
                        if( p_iTagCutoffFrequency <= cTag.getFrequency( ) )
                        {
                            //ds done we can use this datapoint
                            bIsFrequencyAboveCutoff = true;
                            
                            //ds insert tag in tags table for the current cutoff
                            _insertTag( cTag, strTableTags );
                        }
                    }
                    
                    //ds check if we inserted any tags -> we can use the datapoint
                    if( bIsFrequencyAboveCutoff )
                    {
                        //ds determine feature values
                        final int iID_DataPoint   = cPreviousDataPoint.getID( );
                        final boolean bIsAnimated = cPreviousDataPoint.getType( ).equals( "gif" );
                        final boolean bIsPhoto    = cPreviousDataPoint.isPhoto( );
                        final boolean isText      = cPreviousDataPoint.getTextAmount( ) > m_dMinimumText;
                        final boolean isLiked     = ( double )( cPreviousDataPoint.getLikes( ) )/( cPreviousDataPoint.getDislikes( )+1  ) > m_dMinimumLikeDislikeRatio;
                        final boolean isHot       = ( double )( cPreviousDataPoint.getLikes( )+cPreviousDataPoint.getDislikes( ) )/( cPreviousDataPoint.getCountComments( )+1 ) < m_dMaximumVotesCommentsRatio;
                        
                        //ds insert into pattern table
                        _insertPattern( new CPattern( iID_DataPoint, cPreviousDataPoint.getTitle( ), bIsAnimated, bIsPhoto, isText, isLiked, isHot, cPreviousDataPoint.getTags( ) ), strTablePatterns );
                    }
                    else
                    {
                        ++iNumberOfDiscardedPoints;
                    }
                }
                
                //ds new datapoint
                cPreviousDataPoint = _getDataPointFromResultSet( cResultSetDataPoint );
                
                //ds add current tag
                cPreviousDataPoint.getTagsModifiable( ).add( new CTag( cResultSetDataPoint.getInt( "id_tag" ), cResultSetDataPoint.getString( "value" ), cResultSetDataPoint.getInt( "frequency" ) ) );                
            }
        }
        
        //ds check the previous datapoint (tag collection complete) - minimum 1 tag has to reach cutoff frequency
        boolean bIsFrequencyAboveCutoff = false;
        
        //ds we now have to check the tag frequencies
        for( CTag cTag: cPreviousDataPoint.getTags( ) )
        {
            //ds if the frequency is above the threshold
            if( p_iTagCutoffFrequency <= cTag.getFrequency( ) )
            {
                //ds done we can use this datapoint
                bIsFrequencyAboveCutoff = true;
                
                //ds insert tag in tags table for the current cutoff
                _insertTag( cTag, strTableTags );
            }
        }
        
        //ds check if we inserted any tags -> we can use the datapoint
        if( bIsFrequencyAboveCutoff )
        {
            //ds determine feature values
            final int iID_DataPoint   = cPreviousDataPoint.getID( );
            final boolean bIsAnimated = cPreviousDataPoint.getType( ).equals( "gif" );
            final boolean bIsPhoto    = cPreviousDataPoint.isPhoto( );
            final boolean isText      = cPreviousDataPoint.getTextAmount( ) > m_dMinimumText;
            final boolean isLiked     = ( double )( cPreviousDataPoint.getLikes( ) )/( cPreviousDataPoint.getDislikes( )+1  ) > m_dMinimumLikeDislikeRatio;
            final boolean isHot       = ( double )( cPreviousDataPoint.getLikes( )+cPreviousDataPoint.getDislikes( ) )/( cPreviousDataPoint.getCountComments( )+1 ) < m_dMaximumVotesCommentsRatio;
            
            //ds insert into pattern table
            _insertPattern( new CPattern( iID_DataPoint, cPreviousDataPoint.getTitle( ), bIsAnimated, bIsPhoto, isText, isLiked, isHot, cPreviousDataPoint.getTags( ) ), strTablePatterns );
        }
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(createFeatureTable) feature table creation successful - discarded datapoints: " + iNumberOfDiscardedPoints );
    }
    
    /*ds create tags table
    public final void createTagsTable( final int p_iTagCutoffFrequency ) throws SQLException, IOException
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(computeProbabilities) creating tags table for cutoff frequency: " +  p_iTagCutoffFrequency );
        
        //ds table names
        final String strTableTags = "cutoff_" + Integer.toString( p_iTagCutoffFrequency ) + "_tags" ;
        
        //ds retrieve all tags fullfilling the cutoff
        final PreparedStatement cRetrieveTag = m_cMySQLConnection.prepareStatement( "SELECT * FROM `tags` WHERE `frequency` > ( ? )" );
        cRetrieveTag.setInt( 1, p_iTagCutoffFrequency );
        cRetrieveTag.setQueryTimeout( m_iMySQLTimeoutMS );
        
        //ds get the result
        final ResultSet cResultSetTag = cRetrieveTag.executeQuery( );
        
        //ds as long as we have remaining data
        while( cResultSetTag.next( ) )
        {
            //ds write the entry to the new tags table
            final PreparedStatement cStatementInsertTag = m_cMySQLConnection.prepareStatement( "INSERT INTO `" + strTableTags + "` ( `id_tag`, `value`, `frequency` ) VALUES ( ?, ?, ? )" );
            cStatementInsertTag.setInt( 1, cResultSetTag.getInt( "id_tag" ) );
            cStatementInsertTag.setString( 2, cResultSetTag.getString( "value" ) );
            cStatementInsertTag.setInt( 3, cResultSetTag.getInt( "frequency" ) );
            cStatementInsertTag.setQueryTimeout( m_iMySQLTimeoutMS );
            
            //ds commit
            cStatementInsertTag.execute( );
        }
    }*/
    
    //ds compute probabilities
    public final void computeProbabilities( final int p_iTagCutoffFrequency ) throws SQLException, CZEPMySQLManagerException
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(computeProbabilities) computing probabilities for cutoff frequency: " +  p_iTagCutoffFrequency );
        
        //ds table names
        final String strTagCutoff          = Integer.toString( p_iTagCutoffFrequency );
        final String strTableProbabilities = "cutoff_" + strTagCutoff + "_probabilities";
        final String strTablePatterns      = "cutoff_" + strTagCutoff + "_patterns";
        final String strTableTags          = "cutoff_" + strTagCutoff + "_tags";
        
        //ds get total values
        final int iTotalNumberOfPatterns = _getMaxIDFromTable( "id_pattern", strTablePatterns );
        
        //ds simple feature counters
        int iCounterAnimated = 0;
        int iCounterPhoto    = 0;
        int iCounterText     = 0;
        int iCounterLiked    = 0;
        int iCounterHot      = 0;
        
        //ds we have to loop through the whole pattern table
        final PreparedStatement cRetrievePattern = m_cMySQLConnection.prepareStatement( "SELECT * FROM `" + strTablePatterns + "`" );     
        cRetrievePattern.setQueryTimeout( m_iMySQLTimeoutMS );
        
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
        cStatementInsertValues.setQueryTimeout( m_iMySQLTimeoutMS );
        
        //ds commit
        cStatementInsertValues.execute( );
        
        //ds vector with tags
        Vector< CTag > vecTags = new Vector< CTag >( 0 ); 
        
        //ds total number of occurrences
        int iTotalNumberOfTags = 0;
        
        //ds retrieve all tags fullfilling the cutoff
        final PreparedStatement cRetrieveTag = m_cMySQLConnection.prepareStatement( "SELECT * FROM `" + strTableTags + "`" );
        cRetrieveTag.setQueryTimeout( m_iMySQLTimeoutMS );
        
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
            cStatementInsertTag.setQueryTimeout( m_iMySQLTimeoutMS );
            
            //ds commit
            cStatementInsertTag.execute( );
        }
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CMySQLManager>(computeProbabilities) computation complete" );
    }
    
    //ds log to master database
    public final void logMaster( final String p_strUsername, final int p_iSessionID, final String p_strText ) throws SQLException
    {
        //ds simple insertion
        final PreparedStatement cStatementInsertText = m_cMySQLConnection.prepareStatement( "INSERT INTO `log_master` ( `username`, `id_session`, `text` ) VALUE ( ?, ?, ? )" );
        cStatementInsertText.setString( 1, p_strUsername );
        cStatementInsertText.setInt( 2, p_iSessionID );
        cStatementInsertText.setString( 3, p_strText );
        cStatementInsertText.setQueryTimeout( m_iMySQLTimeoutMS );
        cStatementInsertText.execute( );
    }
    
    //ds log to learner database
    public final void logAddPattern( final String p_strUsername, final int p_iSessionID, final CPattern p_cPattern, final boolean p_bIsLiked ) throws SQLException
    {
        //ds simple insertion
        final PreparedStatement cStatementInsertPattern = m_cMySQLConnection.prepareStatement( "INSERT INTO `log_learner` ( `username`, `id_session`, `id_datapoint`, `random`, `liked`, `probability` ) VALUE ( ?, ?, ?, ?, ?, ? )" );
        cStatementInsertPattern.setString( 1, p_strUsername );
        cStatementInsertPattern.setInt( 2, p_iSessionID );
        cStatementInsertPattern.setInt( 3, p_cPattern.getID( ) );
        cStatementInsertPattern.setBoolean( 4, p_cPattern.isRandom( ) );
        cStatementInsertPattern.setBoolean( 5, p_bIsLiked );
        cStatementInsertPattern.setDouble( 6, p_cPattern.getLikeliness( ) );
        cStatementInsertPattern.setQueryTimeout( m_iMySQLTimeoutMS );
        cStatementInsertPattern.execute( );
    }
    
    //ds log to learner database
    public final void logUpdatePattern( final String p_strUsername, final int p_iSessionID, final CPattern p_cPattern, final boolean p_bIsLiked ) throws SQLException
    {
        //ds simple update
        final PreparedStatement cStatementUpdatePattern = m_cMySQLConnection.prepareStatement( "UPDATE `log_learner` SET `liked` = ( ? ) WHERE `username` = ( ? ) AND `id_session` = ( ? ) AND `id_datapoint` = ( ? )" );
        cStatementUpdatePattern.setBoolean( 1, p_bIsLiked );
        cStatementUpdatePattern.setString( 2, p_strUsername );
        cStatementUpdatePattern.setInt( 3, p_iSessionID );
        cStatementUpdatePattern.setInt( 4, p_cPattern.getID( ) );
        cStatementUpdatePattern.setQueryTimeout( m_iMySQLTimeoutMS );
        cStatementUpdatePattern.execute( );
    }
    
    //ds set active user
    public final void setActiveUser( final String p_strUsername ) throws SQLException, CZEPMySQLManagerException
    {
        //ds check if available
        if( isUserAvailable( p_strUsername ) )
        {
            //ds simple deletion
            final PreparedStatement cStatementAddUser = m_cMySQLConnection.prepareStatement( "INSERT INTO `active_users` ( `username` ) VALUE ( ? )" );
            cStatementAddUser.setString( 1, p_strUsername );
            cStatementAddUser.setQueryTimeout( m_iMySQLTimeoutMS );
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
            cStatementRemoveUser.setQueryTimeout( m_iMySQLTimeoutMS );
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
        cRetrieveUser.setQueryTimeout( m_iMySQLTimeoutMS );
        
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
    
    //ds retrieves new session id for username
    public final int getSessionID( final String p_strUsername ) throws SQLException
    {
        //ds query for the username on the master table to get the last session id
        final PreparedStatement cRetrieveLastSession = m_cMySQLConnection.prepareStatement( "SELECT * FROM `log_master` WHERE `username` = ( ? ) ORDER BY `timestamp` DESC LIMIT 1" );
        cRetrieveLastSession.setString( 1, p_strUsername );
        cRetrieveLastSession.setQueryTimeout( m_iMySQLTimeoutMS );
        
        //ds get the result
        final ResultSet cResultSetLastSession = cRetrieveLastSession.executeQuery( );
        
        //ds if we get a result we have to increment the retrieved session id for the new one
        if( cResultSetLastSession.next( ) )
        {
            //ds get the session id
            final int iLastSessionID = cResultSetLastSession.getInt( "id_session" );
            
            //ds return new session id
            return iLastSessionID+1;
        }
        else
        {
            //ds no entry on learners table - new user, new session id is 0
            return 0;
        }
    }
    
    //ds get data growth values
    public final Vector< CPair< Integer, Integer > > getDataGrowth( ) throws SQLException
    {
        //ds result vector
        Vector< CPair< Integer, Integer > > vecImages = new Vector< CPair< Integer, Integer > >( 0 );
        
        //ds query for the first datapoint - implicit inner join
        final PreparedStatement cRetrieveDataPointsAndTags = m_cMySQLConnection.prepareStatement
                                                             ( "SELECT * FROM `datapoints` JOIN mappings ON `datapoints`.`id_datapoint` = `mappings`.`id_datapoint` " +
                                                                                          "JOIN `tags` ON `tags`.`id_tag` = `mappings`.`id_tag`" );
        
        //ds get the result
        final ResultSet cResultSetDataPoint = cRetrieveDataPointsAndTags.executeQuery( );

        //ds counters
        int uNumberOfFeatures = 0;
        //int uNumberOfTagTypes = 0;
        
        //ds previous datapoint info
        int uPreviousDataPoint_ID       = 0;
        int uPreviousDataPoint_IDTagMax = 0;
        
        //ds first datapoint is done ahead to avoid more loop parameters and if clauses
        if( cResultSetDataPoint.next( ) )
        {
            //ds new datapoint
            uPreviousDataPoint_ID       = cResultSetDataPoint.getInt( "id_datapoint" );
            uPreviousDataPoint_IDTagMax = cResultSetDataPoint.getInt( "id_tag" );
            
            //ds do routine to increment counters (trivial here since the first data point)
            ++uNumberOfFeatures;
            //++uNumberOfTagTypes;
        }
        
        //ds as long as we have remaining data
        while( cResultSetDataPoint.next( ) )
        {
            //ds check if pattern id matches previous (we only have to add tags)
            if( uPreviousDataPoint_ID == cResultSetDataPoint.getInt( "id_datapoint" ) )
            {
                //ds increment the features
                ++uNumberOfFeatures; 
                
                //ds get current tag id
                final int iIDTag = cResultSetDataPoint.getInt( "id_tag" );                
                
                //ds check if we got a new tag id (i.e tag id has to be bigger than the last, biggest one set)
                if( iIDTag > uPreviousDataPoint_IDTagMax )
                {
                    //ds also increment the tags
                    //++uNumberOfTagTypes;
                    
                    //ds update the maximum
                    uPreviousDataPoint_IDTagMax = iIDTag;
                }  
            }
            else
            {
                //ds new data point - first add the old one
                vecImages.add( new CPair< Integer, Integer >( uNumberOfFeatures, uPreviousDataPoint_IDTagMax ) );
                
                //ds new datapoint
                uPreviousDataPoint_ID       = cResultSetDataPoint.getInt( "id_datapoint" );
                
                //ds increment total features
                ++uNumberOfFeatures;
                
                //ds get current tag id
                final int iIDTag = cResultSetDataPoint.getInt( "id_tag" );                
                
                //ds check if we got a new tag id (i.e tag id has to be bigger than the last, biggest one set)
                if( iIDTag > uPreviousDataPoint_IDTagMax )
                {
                    //ds also increment the tags
                    //++uNumberOfTagTypes;
                    
                    //ds update the maximum
                    uPreviousDataPoint_IDTagMax = iIDTag;
                }                  
            }
        }
        
        //ds add the last data point
        vecImages.add( new CPair< Integer, Integer >( uNumberOfFeatures, uPreviousDataPoint_IDTagMax ) );
        
        return vecImages;
    }
    
    //ds get possible cutoff values
    public final Vector< Integer > getCutoffValues( ) throws SQLException
    {
        //ds query on the tag table
        final PreparedStatement cRetrieveTags = m_cMySQLConnection.prepareStatement( "SELECT * FROM `tags`" );
        cRetrieveTags.setQueryTimeout( m_iMySQLTimeoutMS ); 
        
        //ds get the result
        final ResultSet cResultSetTags = cRetrieveTags.executeQuery( );
        
        //ds vector with cutoff values, decreasing
        Vector< Integer > vecCutoffValues = new Vector< Integer >( 0 );
        
        //ds for all tags
        while( cResultSetTags.next( ) )
        {
            //ds get the frequency
            final int iFrequency = cResultSetTags.getInt( "frequency" );
            
            //ds check if we dont have an entry yet
            if( !vecCutoffValues.contains( iFrequency ) )
            {
                //ds add the frequency value
                vecCutoffValues.add( iFrequency );
            }
        }
        
        //ds return
        return vecCutoffValues;
    }
    
    //ds get the number of tag types for the desired cutoff
    public final int getNumberOfTagTypesForCutoff( final int p_iTagCutoffFrequency ) throws SQLException
    {
        //ds query on the tag table
        final PreparedStatement cRetrieveTags = m_cMySQLConnection.prepareStatement( "SELECT * FROM `tags` WHERE `frequency` >= ( ? )" );
        cRetrieveTags.setInt( 1, p_iTagCutoffFrequency );
        cRetrieveTags.setQueryTimeout( m_iMySQLTimeoutMS ); 
        
        //ds get the result
        final ResultSet cResultSetTag = cRetrieveTags.executeQuery( );
        
        //ds counter
        int iCounter = 0;
        
        //ds simply count the results (TODO UGLY)
        while( cResultSetTag.next( ) )
        {
            //ds one more entry
            ++iCounter;
        }
        
        //ds return the counter
        return iCounter;
    }
    
    //ds counts datapoints per current cutoff (SLOW)
    public final Map< Integer, Integer > getNumberOfDataPointsForCutoffs( final Vector< Integer > p_vecTagCutoffFrequencies ) throws SQLException, MalformedURLException
    {
        //ds result map with datapoint entries per cutoff
        Map< Integer, Integer > mapDataPointsCutoff = new HashMap< Integer, Integer >( p_vecTagCutoffFrequencies.size( ) );
        
        //ds query for the first datapoint - implicit inner join
        final PreparedStatement cRetrieveDataPointsAndTags = m_cMySQLConnection.prepareStatement
                                                             ( "SELECT * FROM `datapoints` JOIN mappings ON `datapoints`.`id_datapoint` = `mappings`.`id_datapoint` " +
                                                                                          "JOIN `tags` ON `tags`.`id_tag` = `mappings`.`id_tag`" );
        
        //ds get the result
        final ResultSet cResultSetDataPoint = cRetrieveDataPointsAndTags.executeQuery( );

        //ds previous datapoint reference
        CDataPoint cPreviousDataPoint = null;
        
        //ds first datapoint is done ahead to avoid more loop parameters and if clauses
        if( cResultSetDataPoint.next( ) )
        {
            //ds new datapoint (without tags)
            cPreviousDataPoint = new CDataPoint( cResultSetDataPoint.getInt( "id_datapoint" ),
                                                 cResultSetDataPoint.getURL( "url" ),
                                                 cResultSetDataPoint.getString( "title" ),
                                                 cResultSetDataPoint.getString( "type" ),
                                                 cResultSetDataPoint.getInt( "likes" ),
                                                 cResultSetDataPoint.getInt( "dislikes" ),
                                                 cResultSetDataPoint.getInt( "count_comments" ),
                                                 cResultSetDataPoint.getInt( "count_tags" ),
                                                 cResultSetDataPoint.getBoolean( "is_photo" ),
                                                 cResultSetDataPoint.getDouble( "text_amount" ),
                                                 new Vector< CTag >( 0 ) );
            
            //ds add current tag
            cPreviousDataPoint.getTagsModifiable( ).add( new CTag( cResultSetDataPoint.getInt( "id_tag" ), cResultSetDataPoint.getString( "value" ), cResultSetDataPoint.getInt( "frequency" ) ) );
        }
        
        //ds as long as we have remaining data
        while( cResultSetDataPoint.next( ) )
        {
            //ds check if pattern id matches previous (we only have to add tags)
            if( cPreviousDataPoint.getID( ) == cResultSetDataPoint.getInt( "id_datapoint" ) )
            {
                //ds add current tag
                cPreviousDataPoint.getTagsModifiable( ).add( new CTag( cResultSetDataPoint.getInt( "id_tag" ), cResultSetDataPoint.getString( "value" ), cResultSetDataPoint.getInt( "frequency" ) ) );
            }
            else
            {
                //ds check the previous datapoint (tag collection complete) - get maximum frequency
                final int iMaximumFrequency = CTag.getMaximumFrequency( cPreviousDataPoint.getTags( ) );
                
                //ds check over all frequencies (starting at maximum frequency)
                for( int iFrequency: p_vecTagCutoffFrequencies )
                {
                    //ds as soon as we fit in
                    if( iFrequency <= iMaximumFrequency )
                    {
                        //ds add the datapoint - if exists
                        if( mapDataPointsCutoff.containsKey( iFrequency ) )
                        {
                            //ds increase
                            mapDataPointsCutoff.put( iFrequency, mapDataPointsCutoff.get( iFrequency )+1 );
                        }
                        else
                        {
                            //ds first datapoint for this frequency
                            mapDataPointsCutoff.put( iFrequency, 1 );
                        }
                        
                        //ds done for this datapoint -> this line saves a lot of time since we only have to check for the maximum frequency
                        break;
                    }
                }
                
                //ds new datapoint
                cPreviousDataPoint = new CDataPoint( cResultSetDataPoint.getInt( "id_datapoint" ),
                        cResultSetDataPoint.getURL( "url" ),
                        cResultSetDataPoint.getString( "title" ),
                        cResultSetDataPoint.getString( "type" ),
                        cResultSetDataPoint.getInt( "likes" ),
                        cResultSetDataPoint.getInt( "dislikes" ),
                        cResultSetDataPoint.getInt( "count_comments" ),
                        cResultSetDataPoint.getInt( "count_tags" ),
                        cResultSetDataPoint.getBoolean( "is_photo" ),
                        cResultSetDataPoint.getDouble( "text_amount" ),
                        new Vector< CTag >( 0 ) );
                
                //ds add current tag
                cPreviousDataPoint.getTagsModifiable( ).add( new CTag( cResultSetDataPoint.getInt( "id_tag" ), cResultSetDataPoint.getString( "value" ), cResultSetDataPoint.getInt( "frequency" ) ) );                
            }
        }
        
        //ds check the last datapoint (tag collection complete) - get maximum frequency
        final int iMaximumFrequency = CTag.getMaximumFrequency( cPreviousDataPoint.getTags( ) );
        
        //ds check over all frequencies (starting at maximum frequency)
        for( int iFrequency: p_vecTagCutoffFrequencies )
        {
            //ds as soon as we fit in
            if( iFrequency <= iMaximumFrequency )
            {
                //ds add the datapoint - if exists
                if( mapDataPointsCutoff.containsKey( iFrequency ) )
                {
                    //ds increase
                    mapDataPointsCutoff.put( iFrequency, mapDataPointsCutoff.get( iFrequency )+1 );
                }
                else
                {
                    //ds first datapoint for this frequency
                    mapDataPointsCutoff.put( iFrequency, 1 );
                }
                
                //ds done for this datapoint -> this line saves a lot of time since we only have to check for the maximum frequency
                break;
            }
        }
        
        //ds return the counter
        return mapDataPointsCutoff;
    }
    
    //ds counts datapoints per current cutoff (SLOW)
    public final void writeCSVFromLogLearner( final String p_strUsername, final int p_iSessionID ) throws SQLException, IOException
    {
        //ds query on the learners table
        final PreparedStatement cRetrievePickInformation = m_cMySQLConnection.prepareStatement( "SELECT * FROM `log_learner` WHERE `username` = ( ? ) AND `id_session` = ( ? )" );
        cRetrievePickInformation.setString( 1, p_strUsername );
        cRetrievePickInformation.setInt( 2, p_iSessionID );
        cRetrievePickInformation.setQueryTimeout( m_iMySQLTimeoutMS ); 
        
        //ds get the result
        final ResultSet cResultSetPickInformation = cRetrievePickInformation.executeQuery( );
        
        //ds counters
        int iCounterDataPoint = 0;
        int iCounterNettoLike = 0;
        
        //ds open writer
        FileWriter cWriter = new FileWriter( p_strUsername + ".csv", false );
        
        //ds check all retrieved datapoints
        while( cResultSetPickInformation.next( ) )
        {   
            //ds get probability information
            final double dProbability = cResultSetPickInformation.getDouble( "probability" );
            
            //ds binary boolean for logging: 0 false, 1 true
            int iIsRandom = 0;
            
            //ds determine
            if( cResultSetPickInformation.getBoolean( "random" ) ){ iIsRandom = 1; }
            
            //ds get labeling information - if liked
            if( cResultSetPickInformation.getBoolean( "liked" ) )
            {
                //ds positive netto
                ++iCounterNettoLike;
            }
            else
            {
                //ds negative netto
                --iCounterNettoLike;
            }
            
            //ds set info [counter,netto likes, probability, random]
            cWriter.append( iCounterDataPoint + "," + iCounterNettoLike + "," + dProbability + "," + iIsRandom + "\n" );
            
            //ds next datapoint
            ++iCounterDataPoint;
        }
        
        //ds close writer
        cWriter.close( );
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
        final PreparedStatement cStatementCheck = m_cMySQLConnection.prepareStatement( "SELECT MAX(`"+p_strKeyID+"`) FROM `"+p_strTable+"` LIMIT 1" );
        
        //ds set timeout
        cStatementCheck.setQueryTimeout( m_iMySQLTimeoutMS );
        
        //ds retrieve result
        final ResultSet cResultSetCheck = cStatementCheck.executeQuery( );
        
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
    
    //ds inserts tag into table
    private final void _insertTag( final CTag p_cTag, final String p_strTableTags ) throws SQLException
    {
        //ds create an entry in the tags table - check if we already have an entry
        final PreparedStatement cStatementCheckTags = m_cMySQLConnection.prepareStatement( "SELECT * from `" + p_strTableTags + "` WHERE `value` = ( ? ) LIMIT 1" );
        cStatementCheckTags.setString( 1, p_cTag.getValue( ) );
        cStatementCheckTags.setQueryTimeout( m_iMySQLTimeoutMS );
        
        //ds get the result
        final ResultSet cResultTag = cStatementCheckTags.executeQuery( ); 
        
        //ds if there is no entry yet
        if( !cResultTag.next( ) )
        {
            //ds add the tag to the tags table
            final PreparedStatement cStatementInsertTag = m_cMySQLConnection.prepareStatement( "INSERT INTO `" + p_strTableTags + "` ( `id_tag`, `value`, `frequency` ) VALUE ( ?, ?, ? )" );
            cStatementInsertTag.setInt( 1, p_cTag.getID( ) );
            cStatementInsertTag.setString( 2, p_cTag.getValue( ) );
            cStatementInsertTag.setInt( 3, 1 );
            cStatementInsertTag.setQueryTimeout( m_iMySQLTimeoutMS );
            cStatementInsertTag.execute( );
        }
        else
        {
            //ds increment the frequency counter - first obtain it
            final int iCounter = cResultTag.getInt( "frequency" );
            
            //ds insert the incremented counter
            final PreparedStatement cStatementInsertTag = m_cMySQLConnection.prepareStatement( "UPDATE `" + p_strTableTags + "` SET `frequency` = ( ? ) WHERE `value` = ( ? )" );
            cStatementInsertTag.setInt( 1, iCounter+1 );
            cStatementInsertTag.setString( 2, p_cTag.getValue( ) );
            cStatementInsertTag.setQueryTimeout( m_iMySQLTimeoutMS );
            cStatementInsertTag.execute( );                    
        }        
    }
    
    //ds get tag frequencies from learner log (UGLY pair magic TODO substitute by struct)
    public final Vector< CPair< CPair< String, Double >, CPair< Integer, Integer > > > getLearnerTagFrequenciesNormalized( final String p_strTableTags, boolean p_bCountLikes ) throws SQLException, CZEPMySQLManagerException
    {
        //ds return map
        Vector< CPair< CPair< String, Double >, CPair< Integer, Integer > > > vecFrequencies = new Vector< CPair< CPair< String, Double >, CPair< Integer, Integer > > >( 0 ); 
        
        //ds query for the first datapoint - implicit inner join
        final PreparedStatement cRetrieveDataPointsAndTags = m_cMySQLConnection.prepareStatement
                                                             ( "SELECT * FROM `log_learner` JOIN `mappings` ON `log_learner`.`id_datapoint` = `mappings`.`id_datapoint` " +
                                                                                           "JOIN " + p_strTableTags + " ON " + p_strTableTags + ".`id_tag` = `mappings`.`id_tag`" );
        
        //ds get the result
        final ResultSet cResultSetDataPoint = cRetrieveDataPointsAndTags.executeQuery( );
        
        //ds for all datapoints in the learners table
        while( cResultSetDataPoint.next( ) )
        {
            //ds get the tag name and label
            final String strTag  = cResultSetDataPoint.getString( "value" );
            final boolean bLiked = cResultSetDataPoint.getBoolean( "liked" );
            
            //ds check if we are counting likes and got a like or if we are counting dislikes and got a dislike
            if( ( p_bCountLikes && bLiked ) || ( !p_bCountLikes && !bLiked ) )
            {
                //ds check if we already have an entry (the numbers dont matter)
                final int iIndex = vecFrequencies.indexOf( new CPair< CPair< String, Double >, CPair< Integer, Integer > >( new CPair< String, Double >( strTag, 1.0 ), new CPair< Integer, Integer >( 1, 0 ) ) );
                
                //ds if we already have an entry for this tag type
                if( -1 != iIndex )
                {
                    //ds just increment the frequency values
                    vecFrequencies.set( iIndex, new CPair< CPair< String, Double >, CPair< Integer, Integer > >( new CPair< String, Double >( strTag, vecFrequencies.get( iIndex ).A.B+1.0 ), new CPair< Integer, Integer >( vecFrequencies.get( iIndex ).B.A+1, 0 ) ) );
                }
                else
                {
                    //ds create new entry with frequency 1
                    vecFrequencies.add( new CPair< CPair< String, Double >, CPair< Integer, Integer > >( new CPair< String, Double >( strTag, 1.0 ), new CPair< Integer, Integer >( 1, 0 ) ) );
                }
            }
        }
        
        //ds we now have to normalize the tag frequencies by the total frequencies - TODO: this loop is highly inefficient but is kept for readability
        for( CPair< CPair< String, Double >, CPair< Integer, Integer > > cTag: vecFrequencies )
        {
            //ds get the frequency from MySQL
            final PreparedStatement cStatementGetFrequency = m_cMySQLConnection.prepareStatement( "SELECT * from `" + p_strTableTags + "` WHERE `value` = ( ? ) LIMIT 1" );
            cStatementGetFrequency.setString( 1, cTag.A.A );
            cStatementGetFrequency.setQueryTimeout( m_iMySQLTimeoutMS );
            
            //ds get the result
            final ResultSet cResultSetFrequency = cStatementGetFrequency.executeQuery( );
            
            //ds if we got a frequency result
            if( cResultSetFrequency.next( ) )
            {
                //ds get frequency as integer
                final int iFrequency = cResultSetFrequency.getInt( "frequency" );
                
                //ds store in vector
                cTag.B.B = iFrequency;
                
                //ds normalize by that amount
                cTag.A.B = cTag.A.B/iFrequency;
            }
            else
            {
                //ds escape
                throw new CZEPMySQLManagerException( "no frequency found for tag: " + cTag.A.A );
            }
        }
        
        //ds return
        return vecFrequencies;
    }
    
    /*ds helper for data element retrieval
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
        cRetrieveMapping.setQueryTimeout( m_iMySQLTimeoutMS );
        
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
            cRetrieveTag.setQueryTimeout( m_iMySQLTimeoutMS );
            
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
    }*/
    
    //ds helper for pattern retrieval
    private final CDataPoint _getDataPointFromResultSet( final ResultSet p_cResultSetDataPoint ) throws SQLException
    {
        //ds new datapoint
        return new CDataPoint( p_cResultSetDataPoint.getInt( "id_datapoint" ),
                               p_cResultSetDataPoint.getURL( "url" ),
                               p_cResultSetDataPoint.getString( "title" ),
                               p_cResultSetDataPoint.getString( "type" ),
                               p_cResultSetDataPoint.getInt( "likes" ),
                               p_cResultSetDataPoint.getInt( "dislikes" ),
                               p_cResultSetDataPoint.getInt( "count_comments" ),
                               p_cResultSetDataPoint.getInt( "count_tags" ),
                               p_cResultSetDataPoint.getBoolean( "is_photo" ),
                               p_cResultSetDataPoint.getDouble( "text_amount" ),
                               new Vector< CTag >( 0 ) );
    }
    
    //ds helper for pattern retrieval
    private final CPattern _getPatternFromResultSet( final ResultSet p_cResultSetPattern ) throws SQLException
    {
        //ds new pattern
        return new CPattern( p_cResultSetPattern.getInt( "id_datapoint" ),
                             p_cResultSetPattern.getString( "title"),
                             p_cResultSetPattern.getBoolean( "animated" ),
                             p_cResultSetPattern.getBoolean( "photo" ),
                             p_cResultSetPattern.getBoolean( "text" ),
                             p_cResultSetPattern.getBoolean( "liked" ),
                             p_cResultSetPattern.getBoolean( "hot" ),
                             new Vector< CTag >( 0 ) );
    }
    
    //ds pattern insertion
    private final void _insertPattern( final CPattern p_cPattern, final String p_strTablePatterns ) throws SQLException
    {
        //ds get insertion query for the feature table
        final PreparedStatement cStatementInsertPattern = m_cMySQLConnection.prepareStatement( "INSERT INTO `" + p_strTablePatterns + "` ( `id_datapoint`, `title`, `animated`, `photo`, `text`, `liked`, `hot` ) VALUE ( ?, ?, ?, ?, ?, ?, ? )" );

        //ds set params
        cStatementInsertPattern.setInt( 1, p_cPattern.getID( ) );  
        cStatementInsertPattern.setString( 2, p_cPattern.getTitle( ) );  
        cStatementInsertPattern.setBoolean( 3, p_cPattern.isAnimated( ) );
        cStatementInsertPattern.setBoolean( 4, p_cPattern.isPhoto( ) );
        cStatementInsertPattern.setBoolean( 5, p_cPattern.isText( ) );
        cStatementInsertPattern.setBoolean( 6, p_cPattern.isLiked( ) );
        cStatementInsertPattern.setBoolean( 7, p_cPattern.isHot( ) );
        cStatementInsertPattern.setQueryTimeout( m_iMySQLTimeoutMS );

        //ds execute
        cStatementInsertPattern.execute( );
    }
}
