package main;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

//ds custom imports
import learning.CLearner;
import exceptions.CZEPEoIException;
import exceptions.CZEPMySQLManagerException;
import exceptions.CZEPnpIException;
import utility.CDataPoint;
import utility.CImageHandler;
import utility.CLogger;

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
    private final JTextField m_cTextFieldVisits      = new JTextField( 3 );
    private final JTextField m_cTextFieldConfidence  = new JTextField( 3 );
    private final JTextField m_cTextFieldType        = new JTextField( 3 );
    private final JTextField m_cTextFieldDatasetSize = new JTextField( 3 );
    
    //ds interpreter - all calls go over this object
    private final CLearner m_cLearner;
    
    //ds image handler (scaling)
    private final CImageHandler m_cImageHandler;
    
    //ds constructor
    public CGUI( final CLearner p_cLearner, final int p_iWindowWidth, final int p_iWindowHeight )
    {        
        //ds set the learner
        m_cLearner = p_cLearner;
        
        //ds and a image handler
        m_cImageHandler = new CImageHandler( p_iWindowWidth-200, p_iWindowHeight-175 );
        
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
        
        System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(CGUI) CGUI instance allocated" );
    }
    
    //ds enable display
    public void launch( ) throws CZEPMySQLManagerException, SQLException, CZEPEoIException
    {
        //ds allocate a dialog object to display independently
        final JDialog cDialog = new JDialog( m_cFrame, "ZEP: Zero-Effort Procrastination", false );
        
        //ds set the option panel without any options
        cDialog.setContentPane( new JOptionPane( "Loading image data please wait", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null ) );
        
        //ds display the dialog
        cDialog.pack( );
        cDialog.setVisible( true );
        
    	//ds launch the learner
    	m_cLearner.launch( );
    	
        //ds try to get the image for first display
        _displayImage( m_cLearner.getNextImage( CLearner.ELearner.IDC ) );
        
        //ds dispose loading screen
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
            if(      cSource == m_cButtonLike ){ _displayImage( m_cLearner.getNextImage( CLearner.ELearner.LIKE ) ); }
            else if( cSource == m_cButtonDislike ){ _displayImage( m_cLearner.getNextImage( CLearner.ELearner.DISLIKE ) ); }
            else if( cSource == m_cButtonPrevious ){ _displayImage( m_cLearner.getPrevImage( ) ); }
            else if( cSource == m_cButtonReset )
            {
                //ds reset the learning process
                m_cLearner.reset( );
                
                //ds get a new image
                _displayImage( m_cLearner.getNextImage( CLearner.ELearner.IDC ) );
            }
        }
        catch( CZEPEoIException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(actionPerformed) CZEPException: " + e.getMessage( ) );
            
            //ds TODO improve exception handling - right now there will just be no image displayed
            JOptionPane.showMessageDialog( m_cFrame, "Reached end of image sequence - press [Reset] to start over" );           
        }
        catch( CZEPnpIException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(actionPerformed) CZEPException: " + e.getMessage( ) );
            
            //ds no previous image available
            JOptionPane.showMessageDialog( m_cFrame, "No previous image available - please use Like/Dislike to classify the first image" );         
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
    private void _displayImage( final CDataPoint p_cDataPoint )
    {
        try
        {
            //ds get datapoint type
            final String strType = p_cDataPoint.getType( );
            
            //ds see if we got a gif or a regular image
            if( !strType.contains( "gif" ) )
            {
                //ds call image handler to scale the image
                m_cLabelImage.setIcon( m_cImageHandler.getImage( p_cDataPoint.getURL( ) ) );
                
                //ds get text percentage
                m_cImageHandler.getTextPercentageCanny( p_cDataPoint.getURL( ) );
            }
            else
            {
                //ds load the image directly
                m_cLabelImage.setIcon( new ImageIcon( new URL( p_cDataPoint.getURL( ) ) ) );                
            }
            
            //ds update image info
            m_cTextFieldTitle.setText( p_cDataPoint.getTitle( ) );
            m_cTextFieldURL.setText( p_cDataPoint.getURL( ) );
            m_cTextFieldTags.setText( p_cDataPoint.getTags( ).toString( ) );
            
            //ds side panel
            m_cTextFieldImageID.setText( Integer.toString( p_cDataPoint.getLinkedID( ) ) );
            m_cTextFieldVisits.setText( Integer.toString( m_cLearner.getNumberOfVisits( ) ) );
            m_cTextFieldType.setText( p_cDataPoint.getType( ) );
            m_cTextFieldDatasetSize.setText( Integer.toString( m_cLearner.getDatasetSize( ) ) );
            m_cTextFieldLikes.setText( Integer.toString( m_cLearner.getNumberOfLikes( ) ) );
        }
        catch( IOException e )
        {
            System.out.println( "[" + CLogger.getStamp( ) + "]<CGUI>(_displayImage) IOException: " + e.getMessage( ) );
            
            //ds TODO improve exception handling - right now there will just be no image displayed
            JOptionPane.showMessageDialog( m_cFrame, "Could not display URL:\n" + p_cDataPoint.getURL( ) );
        }  
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
        m_cTextFieldURL.setEditable( false );
        m_cTextFieldTags.setEditable( false );
        m_cTextFieldImageID.setEditable( false );
        m_cTextFieldLikes.setEditable( false );
        m_cTextFieldVisits.setEditable( false );
        m_cTextFieldConfidence.setEditable( false );
        m_cTextFieldType.setEditable( false );
        m_cTextFieldDatasetSize.setEditable( false );
        
        //ds add the sidebar panels
        final JPanel cPanelImageID     = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelLikes       = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelVisits      = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelConfidence  = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelType        = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        final JPanel cPanelDatasetSize = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        
        //ds add labels and textfield to each panel
        cPanelImageID.add( new JLabel( "MySQL ID: " ) );         cPanelImageID.add( m_cTextFieldImageID );
        cPanelLikes.add( new JLabel( "Total Likes: " ) );        cPanelLikes.add( m_cTextFieldLikes );
        cPanelVisits.add( new JLabel( "Visited Images: " ) );    cPanelVisits.add( m_cTextFieldVisits );
        cPanelConfidence.add( new JLabel( "Confidence: " ) );    cPanelConfidence.add( m_cTextFieldConfidence );
        cPanelType.add( new JLabel( "Type: " ) );                cPanelType.add( m_cTextFieldType );
        cPanelDatasetSize.add( new JLabel( "Dataset Size: " ) ); cPanelDatasetSize.add( m_cTextFieldDatasetSize );
        
        //ds set maximum size to align vertically
        cPanelImageID.setMaximumSize( cPanelImageID.getPreferredSize( ) );
        cPanelLikes.setMaximumSize( cPanelLikes.getPreferredSize( ) );
        cPanelVisits.setMaximumSize( cPanelVisits.getPreferredSize( ) );
        cPanelConfidence.setMaximumSize( cPanelConfidence.getPreferredSize( ) );
        cPanelType.setMaximumSize( cPanelType.getPreferredSize( ) );
        cPanelDatasetSize.setMaximumSize( cPanelDatasetSize.getPreferredSize( ) );
        
        //ds align right horizontally
        cPanelImageID.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelLikes.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelVisits.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelConfidence.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelType.setAlignmentX( Component.RIGHT_ALIGNMENT );
        cPanelDatasetSize.setAlignmentX( Component.RIGHT_ALIGNMENT );
        
        //ds north panels
        final JPanel cPanelTitle = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
        final JPanel cPanelURL   = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
        final JPanel cPanelTags  = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
        
        //ds connect
        cPanelTitle.add( new JLabel( " Title: " ) ); cPanelTitle.add( m_cTextFieldTitle );
        cPanelURL.add( new JLabel( "  URL: " ) );    cPanelURL.add( m_cTextFieldURL );
        cPanelTags.add( new JLabel( "Tags: " ) );   cPanelTags.add( m_cTextFieldTags );
        
        //ds setup the main panels
        m_cPanelNorth.setLayout( new BoxLayout( m_cPanelNorth, BoxLayout.Y_AXIS ) );
        m_cPanelNorth.setAlignmentX( Component.LEFT_ALIGNMENT );
        m_cPanelNorth.add( cPanelTitle );
        m_cPanelNorth.add( cPanelURL );
        m_cPanelNorth.add( cPanelTags );
        
        m_cPanelEast.setLayout( new BoxLayout( m_cPanelEast, BoxLayout.Y_AXIS ) );
        m_cPanelEast.add( cPanelImageID );
        m_cPanelEast.add( cPanelType );
        m_cPanelEast.add( cPanelDatasetSize );
        m_cPanelEast.add( cPanelVisits );
        m_cPanelEast.add( cPanelLikes );
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
