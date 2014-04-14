package main;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.Border;

import learning.CLearnerBayes;
//ds custom imports
//import learning.CLearnerRandom;
import exceptions.CZEPEoIException;
import exceptions.CZEPGUIException;
import exceptions.CZEPLearnerException;
import exceptions.CZEPMySQLManagerException;
import exceptions.CZEPnpIException;
import utility.CDataPoint;
import utility.CImageHandler;
import utility.CLogger;
import utility.CMySQLManager;

public final class CGUI implements ActionListener, KeyListener
{
    //ds GUI frame handle for manipulation
    JFrame m_cFrame = new JFrame( "ZEP: Zero-Effort Procrastination" );
    
    //ds image display
    private final JLabel m_cLabelImage = new JLabel( "", JLabel.CENTER );
    
    //ds panels
    private final JPanel m_cPanelNorth       = new JPanel( );
    private final JPanel m_cPanelEast        = new JPanel( );
    private final JPanel m_cPanelSouth       = new JPanel( );
    private final JPanel m_cPanelWest        = new JPanel( );
    private final JScrollPane m_cPanelCImage = new JScrollPane( m_cLabelImage );
    
    //ds components
    private final JButton m_cButtonReset    = new JButton( "Reset (R)" );;
    private final JButton m_cButtonLike     = new JButton( "Like (>)" );
    private final JButton m_cButtonDislike  = new JButton( "Dislike (v)" );
    private final JButton m_cButtonPrevious = new JButton( "Previous (<)" );
    
    //ds textfields
    private final JTextField m_cTextFieldTitle       = new JTextField( 60 );
    private final JTextField m_cTextFieldURL         = new JTextField( 60 );
    private final JTextField m_cTextFieldTags        = new JTextField( 60 );
    private final JTextField m_cTextFieldImageID     = new JTextField( 3 );
    private final JTextField m_cTextFieldLikes       = new JTextField( 3 );
    private final JTextField m_cTextFieldDislikes    = new JTextField( 3 );
    private final JTextField m_cTextFieldVisits      = new JTextField( 3 );
    private final JTextField m_cTextFieldConfidence  = new JTextField( 3 );
    private final JTextField m_cTextFieldType        = new JTextField( 3 );
    private final JTextField m_cTextFieldDatasetSize = new JTextField( 3 );
    private final JTextField m_cTextFieldTextPercent = new JTextField( 3 );
    private final JTextField m_cTextFieldIsPhoto     = new JTextField( 3 );
    private final JTextField m_cTextFieldComments    = new JTextField( 3 );
    private final JTextField m_cTextFieldTagsCount   = new JTextField( 3 );
    private final JTextField m_cTextFieldTotalLikes  = new JTextField( 3 );
    
    //ds fonts
    Font m_cFontTitle = new Font( m_cTextFieldTitle.getFont( ).getName( ), Font.BOLD, 18 );
    
    //ds interpreter - all calls go over this object
    //private final CLearnerRandom m_cLearner;
    private final CLearnerBayes m_cLearner;
    
    //ds MySQL calls
    private final CMySQLManager m_cMySQLManager;
    
    //ds currently active datapoint
    private CDataPoint m_cCurrentDataPoint = null;
    
    //ds window information
    private final int m_iImageDisplayWidth;
    private final int m_iImageDisplayHeight;
    
    //ds constructor
    public CGUI( final CLearnerBayes p_cLearner, final CMySQLManager p_cMySQLManager, final int p_iWindowWidth, final int p_iWindowHeight )
    {        
        //ds set the learner
        m_cLearner = p_cLearner;
        
        //ds and MySQL manager
        m_cMySQLManager = p_cMySQLManager;
        
        //ds set the window properties
        m_iImageDisplayWidth  = p_iWindowWidth-200;
        m_iImageDisplayHeight = p_iWindowHeight-200;
        
        //ds configure the frame
        m_cFrame.setSize( p_iWindowWidth, p_iWindowHeight );
        //m_cFrame.setResizable( false );
        m_cFrame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        
        //ds divide frame in two parts: information and image
        m_cFrame.setLayout( new BorderLayout( ) );
        
        //ds creating a border to highlight the JPanel areas
        Border cOutline = BorderFactory.createLineBorder( Color.black );
        
        //ds set borders
        //m_cPanelNorth.setBorder( cOutline );
        //m_cPanelEast.setBorder( cOutline );
        //m_cPanelSouth.setBorder( cOutline );
        //m_cPanelWest.setBorder( cOutline );
        m_cPanelCImage.setBorder( cOutline );
        
        //ds setup components (buttons, ..)
        _setComponents( );
        
        //ds add the panels to the frame
        m_cFrame.add( m_cPanelNorth , BorderLayout.NORTH );
        m_cFrame.add( m_cPanelEast  , BorderLayout.EAST );
        m_cFrame.add( m_cPanelSouth , BorderLayout.SOUTH );
        m_cFrame.add( m_cPanelWest  , BorderLayout.WEST );
        m_cFrame.add( m_cPanelCImage, BorderLayout.CENTER );
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(CGUI) Instance allocated" );
    }
    
    //ds enable display
    public void launch( ) throws CZEPGUIException
    {
        //ds allocate a dialog object to display independently
        final JDialog cDialog = new JDialog( m_cFrame, "ZEP: Zero-Effort Procrastination", false );
        
        //ds set the option panel without any options
        cDialog.setContentPane( new JOptionPane( "Loading image data please wait", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null ) );
        
        //ds display the dialog
        cDialog.pack( );
        cDialog.setVisible( true );
        
        try
        {
            //ds fetch initial datapool
            m_cLearner.fetchInitialDataPool( );
    	
            //ds try to get the image for first display
            _displayImage( m_cLearner.getFirstDataPoint( ) );
        }
        catch( Exception e )
        {
            //ds info
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(launch) Exception: " + e.getMessage( ) + " - could not fetch database" );
            
            //ds dispose loading screen
            cDialog.removeAll( );
            cDialog.dispose( );   
            
            //ds rethrow
            throw new CZEPGUIException( "GUI aborted" );
        }
        
        //ds dispose loading screen
        cDialog.removeAll( );
        cDialog.dispose( );
        
        //ds register key listener
        m_cFrame.addKeyListener( this );
        
        //ds display frame for interaction
        m_cFrame.setVisible( true );  

        //ds request focus for key strokes
        m_cFrame.requestFocus( );
    }
    
    //ds check if active
    public boolean isActive( )
    {
        return m_cFrame.isShowing( );
    }
  
    //ds event invoked function
    public void actionPerformed( ActionEvent p_cEvent )
    {
        //ds get the event source
        final Object cSource = p_cEvent.getSource( );
        
        try
        {
            //ds determine event (big if/else tree)
            if(      cSource == m_cButtonLike ){ _displayImage( m_cLearner.getNextDataPoint( CLearnerBayes.ELearnerLabel.LIKE, m_cCurrentDataPoint ) ); }
            else if( cSource == m_cButtonDislike ){ _displayImage( m_cLearner.getNextDataPoint( CLearnerBayes.ELearnerLabel.DISLIKE, m_cCurrentDataPoint ) ); }
            else if( cSource == m_cButtonPrevious ){ _displayImage( m_cLearner.getPreviousDataPoint( ) ); }
            else if( cSource == m_cButtonReset )
            {
                //ds reset the learning process
                m_cLearner.reset( );
                
                //ds get a new image
                _displayImage( m_cLearner.getFirstDataPoint( ) );
            }
        }
        catch( SQLException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(actionPerformed) CZEPMySQLManagerException: " + e.getMessage( ) );
            
            //ds no previous image available
            JOptionPane.showMessageDialog( m_cFrame, "Could not load image from MySQL database" );            
        }
        catch( CZEPnpIException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(actionPerformed) CZEPnpIException: " + e.getMessage( ) );
            
            //ds no previous image available
            JOptionPane.showMessageDialog( m_cFrame, "No previous image available - please use Like/Dislike to classify the first image" );         
        }
        catch( CZEPMySQLManagerException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(actionPerformed) CZEPMySQLManagerException: " + e.getMessage( ) );
            
            //ds no previous image available
            JOptionPane.showMessageDialog( m_cFrame, "Could not load image from MySQL database" );         
        }
        catch( MalformedURLException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(actionPerformed) MalformedURLException: " + e.getMessage( ) );
            
            //ds no previous image available
            JOptionPane.showMessageDialog( m_cFrame, "Could not load image from MySQL database" );                
        }
        catch( CZEPEoIException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(actionPerformed) CZEPEoIException: " + e.getMessage( ) );
            
            //ds no previous image available
            JOptionPane.showMessageDialog( m_cFrame, "Could not load image from MySQL database" );                
        }
        catch( CZEPLearnerException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(actionPerformed) CZEPLearnerException: " + e.getMessage( ) );
            
            //ds no previous image available
            JOptionPane.showMessageDialog( m_cFrame, "Internal learning error - please reset" );                
        }
    }
    
    //ds KeyListener - just map the keys to the buttons
    public void keyPressed( KeyEvent p_cKey )
    {
        //ds get the keycode
        final int iKeyCode = p_cKey.getKeyCode( );
        
        //ds if/else tree
        if(      iKeyCode == KeyEvent.VK_RIGHT ){ m_cButtonLike.doClick( ); }
        else if( iKeyCode == KeyEvent.VK_DOWN ){ m_cButtonDislike.doClick( ); }
        else if( iKeyCode == KeyEvent.VK_LEFT ){ m_cButtonPrevious.doClick( ); }
        else if( iKeyCode == KeyEvent.VK_R ){ m_cButtonReset.doClick( ); }
        else if( iKeyCode == KeyEvent.VK_ESCAPE )
        {
            //ds notify
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(keyPressed) Caught escape signal - shutting down GUI" );
            
            //ds dispose frame
            m_cFrame.dispose( );
        }
    }
    
    //ds update GUI with new image
    private void _displayImage( final CDataPoint p_cDataPoint ) throws CZEPMySQLManagerException
    {
        //ds update active datapoint
        m_cCurrentDataPoint = p_cDataPoint;
        
    	//ds get the extension
    	final String strExtension = p_cDataPoint.getType( );
    	
    	//ds if we got a gif we take the image as is
    	if( strExtension.matches( "gif" ) )
    	{
    		//ds set the icon
    		m_cLabelImage.setIcon( m_cMySQLManager.getImageIcon( p_cDataPoint ) );
    	}
    	else
    	{
    		//ds set the image to the GUI field (resized)
    		m_cLabelImage.setIcon( new ImageIcon( CImageHandler.getResizedImage( m_cMySQLManager.getBufferedImage( p_cDataPoint ), m_iImageDisplayWidth, m_iImageDisplayHeight ) ) );
    	}
        
        //ds update image info
        m_cTextFieldTitle.setText( p_cDataPoint.getTitle( ) );
        m_cTextFieldURL.setText( p_cDataPoint.getURL( ).toString( ) );
        m_cTextFieldTags.setText( p_cDataPoint.getTags( ).toString( ) );
        
        //ds datapoint properties
        m_cTextFieldImageID.setText( Integer.toString( p_cDataPoint.getID( ) ) );
        m_cTextFieldType.setText( p_cDataPoint.getType( ) );
        m_cTextFieldLikes.setText( Integer.toString( p_cDataPoint.getLikes( ) ) );
        m_cTextFieldDislikes.setText( Integer.toString( p_cDataPoint.getDislikes( ) ) );
        
        //ds check if gif
        if( strExtension.matches( "gif" ) )
        {
            //ds no detection done
            m_cTextFieldTextPercent.setText( "" );
            m_cTextFieldIsPhoto.setText( "" );
        }
        else
        {
            //ds get the values
            m_cTextFieldTextPercent.setText( String.format( "%3.2f", p_cDataPoint.getTextAmount( ) ) );
            m_cTextFieldIsPhoto.setText( Boolean.toString( p_cDataPoint.isPhoto( ) ) );            
        }
        
        
        m_cTextFieldComments.setText( Integer.toString( p_cDataPoint.getCountComments( ) ) );
        m_cTextFieldTagsCount.setText( Integer.toString( p_cDataPoint.getCountTags( ) ) );
        
        //ds learner
        m_cTextFieldVisits.setText( Integer.toString( m_cLearner.getNumberOfVisits( ) ) );
        m_cTextFieldDatasetSize.setText( Integer.toString( m_cLearner.getOperations( ) ) );
        m_cTextFieldTotalLikes.setText( Integer.toString( m_cLearner.getNumberOfLikes( ) ) );
    }
    
    //ds component setup
    private void _setComponents( )
    {
        //ds register the buttons
        m_cButtonReset.addActionListener( this );
        m_cButtonReset.addKeyListener( this );
        m_cButtonDislike.addActionListener( this );
        m_cButtonDislike.addKeyListener( this );
        m_cButtonLike.addActionListener( this );
        m_cButtonLike.addKeyListener( this );
        m_cButtonPrevious.addActionListener( this );
        m_cButtonPrevious.addKeyListener( this );
        
        //ds customize mechanical buttons
        m_cButtonReset.setPreferredSize( new Dimension( 120, 25 ) );
        m_cButtonPrevious.setPreferredSize( new Dimension( 120, 25 ) );
        
        //ds customize like buttons
        m_cButtonDislike.setBackground( Color.red );
        m_cButtonLike.setBackground( Color.green );
        m_cButtonDislike.setPreferredSize( new Dimension( 105, 40 ) );
        m_cButtonLike.setPreferredSize( new Dimension( 105, 40 ) );
        
        //ds textfields
        m_cTextFieldTitle.setEditable( false );
        m_cTextFieldTitle.setFont( m_cFontTitle );
        m_cTextFieldURL.setEditable( false );
        m_cTextFieldTags.setEditable( false );
        m_cTextFieldImageID.setEditable( false );
        m_cTextFieldLikes.setEditable( false );
        m_cTextFieldDislikes.setEditable( false );
        m_cTextFieldVisits.setEditable( false );
        m_cTextFieldConfidence.setEditable( false );
        m_cTextFieldType.setEditable( false );
        m_cTextFieldDatasetSize.setEditable( false );
        m_cTextFieldTextPercent.setEditable( false );
        m_cTextFieldIsPhoto.setEditable( false );
        m_cTextFieldComments.setEditable( false );
        m_cTextFieldTagsCount.setEditable( false );
        m_cTextFieldTotalLikes.setEditable( false );
        
        //ds add the sidebar panels
        final JPanel cPanelImageID     = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelLikes       = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelDislikes    = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelVisits      = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelConfidence  = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelType        = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelDatasetSize = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelTextPercent = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelIsPhoto     = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelComments    = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelTagsCount   = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelTotalLikes  = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        
        //ds structural side bar
        final JPanel cPanelProperties = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelLearning   = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelWhitespace = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        
        //ds add labels and textfield to each panel
        cPanelImageID.add( new JLabel( "MySQL ID: " ) );        cPanelImageID.add( m_cTextFieldImageID );
        cPanelLikes.add( new JLabel( "Likes: " ) );             cPanelLikes.add( m_cTextFieldLikes );
        cPanelDislikes.add( new JLabel( "Dislikes: " ) );       cPanelDislikes.add( m_cTextFieldDislikes );
        cPanelVisits.add( new JLabel( "Visited Images: " ) );   cPanelVisits.add( m_cTextFieldVisits );
        cPanelConfidence.add( new JLabel( "Confidence: " ) );   cPanelConfidence.add( m_cTextFieldConfidence );
        cPanelType.add( new JLabel( "Type: " ) );               cPanelType.add( m_cTextFieldType );
        cPanelDatasetSize.add( new JLabel( "Operations: " ) );  cPanelDatasetSize.add( m_cTextFieldDatasetSize );
        cPanelTextPercent.add( new JLabel( "Text Amount: " ) ); cPanelTextPercent.add( m_cTextFieldTextPercent );
        cPanelIsPhoto.add( new JLabel( "Photograph: " ) );      cPanelIsPhoto.add( m_cTextFieldIsPhoto );
        cPanelComments.add( new JLabel( "Comments: " ) );       cPanelComments.add( m_cTextFieldComments );
        cPanelTagsCount.add( new JLabel( "Tags Count: " ) );    cPanelTagsCount.add( m_cTextFieldTagsCount );
        cPanelTotalLikes.add( new JLabel( "Total Likes: " ) );  cPanelTotalLikes.add( m_cTextFieldTotalLikes );
        
        cPanelProperties.add( new JLabel( "Properties                    " ) );
        cPanelLearning.add(   new JLabel( "Learning                       " ) );
        cPanelWhitespace.add( new JLabel( "" ) );
        
        //ds set maximum size to align vertically
        cPanelImageID.setMaximumSize( cPanelImageID.getPreferredSize( ) );
        cPanelLikes.setMaximumSize( cPanelLikes.getPreferredSize( ) );
        cPanelDislikes.setMaximumSize( cPanelDislikes.getPreferredSize( ) );
        cPanelVisits.setMaximumSize( cPanelVisits.getPreferredSize( ) );
        cPanelConfidence.setMaximumSize( cPanelConfidence.getPreferredSize( ) );
        cPanelType.setMaximumSize( cPanelType.getPreferredSize( ) );
        cPanelDatasetSize.setMaximumSize( cPanelDatasetSize.getPreferredSize( ) );
        cPanelTextPercent.setMaximumSize( cPanelTextPercent.getPreferredSize( ) );
        cPanelIsPhoto.setMaximumSize( cPanelIsPhoto.getPreferredSize( ) );
        cPanelComments.setMaximumSize( cPanelComments.getPreferredSize( ) );
        cPanelTagsCount.setMaximumSize( cPanelTagsCount.getPreferredSize( ) );
        cPanelTotalLikes.setMaximumSize( cPanelTotalLikes.getPreferredSize( ) );
        
        cPanelProperties.setMaximumSize( cPanelProperties.getPreferredSize( ) );
        cPanelLearning.setMaximumSize( cPanelLearning.getPreferredSize( ) );
        cPanelWhitespace.setMaximumSize( cPanelWhitespace.getPreferredSize( ) );
        
        //ds align right horizontally
        cPanelImageID.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelLikes.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelDislikes.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelVisits.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelConfidence.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelType.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelDatasetSize.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelTextPercent.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelIsPhoto.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelComments.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelTagsCount.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelTotalLikes.setAlignmentX( Component.RIGHT_ALIGNMENT );
        
        cPanelProperties.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelLearning.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelWhitespace.setAlignmentX( Component.RIGHT_ALIGNMENT );
        
        cPanelProperties.setBorder( BorderFactory.createLineBorder( Color.blue ) );
        cPanelLearning.setBorder( BorderFactory.createLineBorder( Color.blue ) );
        
        //ds north panels
        final JPanel cPanelTitle = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
        final JPanel cPanelURL   = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
        final JPanel cPanelTags  = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
        //final JPanel cPanelFlowWhitespace = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
        
        //ds connect
        cPanelTitle.add( new JLabel( " Title: " ) ); cPanelTitle.add( m_cTextFieldTitle );
        cPanelURL.add( new JLabel( "  URL: " ) );    cPanelURL.add( m_cTextFieldURL );
        cPanelTags.add( new JLabel( "Tags: " ) );   cPanelTags.add( m_cTextFieldTags );
        
        //ds setup the main panels
        m_cPanelNorth.setLayout( new BoxLayout( m_cPanelNorth, BoxLayout.Y_AXIS ) );
        m_cPanelNorth.setAlignmentX( Component.LEFT_ALIGNMENT );
        m_cPanelNorth.add( cPanelURL );
        m_cPanelNorth.add( cPanelTags );
        //m_cPanelNorth.add( cPanelFlowWhitespace );
        m_cPanelNorth.add( cPanelTitle );
        
        //ds side panel
        m_cPanelEast.setLayout( new BoxLayout( m_cPanelEast, BoxLayout.Y_AXIS ) );
        m_cPanelEast.add( cPanelProperties );
        m_cPanelEast.add( cPanelImageID );
        m_cPanelEast.add( cPanelType );
        m_cPanelEast.add( cPanelTextPercent );
        m_cPanelEast.add( cPanelIsPhoto );
        m_cPanelEast.add( cPanelLikes );
        m_cPanelEast.add( cPanelDislikes );
        m_cPanelEast.add( cPanelComments );
        m_cPanelEast.add( cPanelTagsCount );
        
        m_cPanelEast.add( cPanelWhitespace );
        
        m_cPanelEast.add( cPanelLearning );
        m_cPanelEast.add( cPanelDatasetSize );
        m_cPanelEast.add( cPanelVisits );
        m_cPanelEast.add( cPanelTotalLikes );
        m_cPanelEast.add( cPanelConfidence );
        
        m_cPanelSouth.add( m_cButtonReset );
        m_cPanelSouth.add( m_cButtonDislike );
        m_cPanelSouth.add( m_cButtonLike );
        m_cPanelSouth.add( m_cButtonPrevious );   
    }

    //ds virtuals
    public void keyReleased( KeyEvent p_cKey ){ /*ds nothing to do*/ }
    public void keyTyped(KeyEvent p_cKey ){ /*ds nothing to do*/ }
}
