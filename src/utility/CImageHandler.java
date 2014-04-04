package utility;

import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.imgscalr.Scalr;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

public class CImageHandler
{
    //ds attributes
    private int m_iWindowWidth;
    private int m_iWindowHeight;
    
    //ds constructor
    public CImageHandler( int p_iWindowWidth, int p_iWindowHeight )
    {
        System.out.println( "[" + CLogger.getStamp( ) + "]<CImageHandler>(CImageHandler) CImageHandler instance allocated" );
        
        //ds and window dimensions (required for image resizing)
        m_iWindowWidth  = p_iWindowWidth;
        m_iWindowHeight = p_iWindowHeight;
        
    	//ds load OpenCV core
    	System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
    }
    
    //ds specific getter
    public ImageIcon getImage( final String p_strURL ) throws IOException
    {
        //ds acquire the image
        BufferedImage cImage = ImageIO.read( new URL( p_strURL ) );
        
        //ds return resized image
        return new ImageIcon( _resizeImage( cImage ) );         
    }
    
    private BufferedImage _resizeImage( BufferedImage p_cImage )
    {
        //ds get size information
        final int iWidth  = p_cImage.getWidth( );
        final int iHeight = p_cImage.getHeight( );
        
        //ds get ratios
        final double dRatioWidth  = ( double ) iWidth/m_iWindowWidth;
        final double dRatioHeight = ( double ) iHeight/m_iWindowHeight;
        
        //ds dont scale for ratios above 3.0
         if( 3.0 < dRatioWidth || 3.0 < dRatioHeight ){ return p_cImage; }
        
        //ds check which side we have to scale
        if( dRatioWidth > dRatioHeight )
        {
            //ds scale image to width
            p_cImage = Scalr.resize( p_cImage, Scalr.Mode.FIT_TO_WIDTH, m_iWindowWidth, m_iWindowHeight );                
        }
        else
        {
            //ds scale image to height
            p_cImage = Scalr.resize( p_cImage, Scalr.Mode.FIT_TO_HEIGHT, m_iWindowWidth, m_iWindowHeight );  
        }
        
        return p_cImage;
    }
    
    //ds convert from bufferedimage to mat
    private Mat _getMatFromBufferedImage( final BufferedImage p_cImage )
    {
    	//ds get raw data
    	byte[] btRawData = ( ( DataBufferByte ) p_cImage.getRaster( ).getDataBuffer( ) ).getData( );

        //ds allocate a matrix
        Mat matImage = new Mat( p_cImage.getHeight( ), p_cImage.getWidth( ), CvType.CV_8UC3 );
        
        //ds fill the raw data into the matrix
        matImage.put( 0, 0, btRawData );

        //ds return
        return matImage;
    }
    
    //ds compute text percentage
    public final double getTextPercentageCanny( final String p_strURL ) throws IOException
    {
        //ds acquire the image
        BufferedImage cImage = ImageIO.read( new URL( p_strURL ) );

    	//ds allocate image instances (all in grayscale)
    	Mat matImageMarked    = _getMatFromBufferedImage( cImage );
    	Mat matImageGrayScale = Mat.zeros( matImageMarked.size( ), CvType.CV_8UC1 );
    	Mat matImageCanny     = Mat.zeros( matImageMarked.size( ), CvType.CV_8UC1 );
    	
        //ds get the grayscale representation
        Imgproc.cvtColor( matImageMarked, matImageGrayScale, Imgproc.COLOR_RGB2GRAY );
        
        Highgui.imwrite("1grayscale.jpg", matImageGrayScale );
        
        //ds allocate structure element to match
        Mat matStructure = Imgproc.getStructuringElement( Imgproc.CV_SHAPE_RECT, new Size( 21, 3 ) );
        
        //ds tophat filter 
        Imgproc.morphologyEx( matImageGrayScale, matImageGrayScale, Imgproc.MORPH_TOPHAT, matStructure );
        
        Highgui.imwrite("2tophat.jpg", matImageGrayScale );

        //ds threshold
        Imgproc.threshold( matImageGrayScale, matImageGrayScale, 125, 255, Imgproc.THRESH_BINARY );
        
        Highgui.imwrite("3threshold.jpg", matImageGrayScale );
        
        //ds smooth the image
        Imgproc.GaussianBlur( matImageGrayScale, matImageGrayScale, new Size( 3, 3 ), 0 );
        
        Highgui.imwrite("4smooth.jpg", matImageGrayScale );
        
        //ds dilate the structure
        Imgproc.dilate( matImageGrayScale, matImageGrayScale, matStructure );
        
        Highgui.imwrite("5dilate.jpg", matImageGrayScale );
        
        //ds threshold again
        Imgproc.threshold( matImageGrayScale, matImageGrayScale, 200, 255, Imgproc.THRESH_BINARY );
        
        Highgui.imwrite("6threshold2.jpg", matImageGrayScale );
        
        //ds execute canny edge detection
        Imgproc.Canny( matImageGrayScale, matImageGrayScale, 500, 1000 );
        
        //ds allocate mat list
        List< MatOfPoint > vecContours = new ArrayList< MatOfPoint >( );
        
        //ds find the contours
        Imgproc.findContours( matImageGrayScale, vecContours, new Mat( ), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE );
        
        //ds draw all contours in white
        Imgproc.drawContours( matImageCanny, vecContours, -1, new Scalar( 255, 255, 255 ), 1 );
        
        Highgui.imwrite("8contours.jpg", matImageCanny );
        
        //ds rectangle vector
        Vector< Rect > vecTextBoxes = new Vector< Rect >( );
        
        //ds get the rectangles from the contours and check them
        for( MatOfPoint matPoints : vecContours )
        {
        	//ds add the box
        	vecTextBoxes.add( Imgproc.boundingRect( matPoints ) );	
        }
        
        //ds matrix for box display
        Mat matBoxes1 = matImageGrayScale.clone( );
        Mat matBoxes2 = matImageGrayScale.clone( );
        
        //ds draw all boxes
        for( Rect cTextBox : vecTextBoxes )
        {
        	//ds compute corner points for the rectangle to draw
        	final Point cBR = new Point( cTextBox.x, cTextBox.y );
        	final Point cTL = new Point( cTextBox.x+cTextBox.width, cTextBox.y+cTextBox.height );
        	
        	Core.rectangle( matBoxes1, cBR, cTL, new Scalar( 255, 255, 255 ), 2 );        	
        }
        
        //ds save image
        Highgui.imwrite("9boxes_raw.jpg", matBoxes1 );
        
    	System.out.println( "size 1: " + vecTextBoxes.size( ) );
        
        //ds remove double boxes
        vecTextBoxes = _removeDoubleBoxes( vecTextBoxes );
        
    	System.out.println( "size 2: " + vecTextBoxes.size( ) );
        
        //ds now fix bad boxes and connect them (contours connected to the border of the image, split up)
    	vecTextBoxes = _removeInnerBoxes( vecTextBoxes );
        
    	System.out.println( "size 3: " + vecTextBoxes.size( ) );
    	
    	//ds fix border boxes
    	vecTextBoxes = _fixBorderBoxes( vecTextBoxes );
    	
    	System.out.println( "size 4: " + vecTextBoxes.size( ) );
    	
    	//ds finally connect boxes
    	//vecTextBoxes = _connectBoxes( vecTextBoxes );
    	
    	//System.out.println( "size 5: " + vecTextBoxes.size( ) );
    	
        //ds draw all boxes
        for( Rect cTextBox : vecTextBoxes )
        {
        	//ds compute corner points for the rectangle to draw
        	final Point cBR = new Point( cTextBox.x, cTextBox.y );
        	final Point cTL = new Point( cTextBox.x+cTextBox.width, cTextBox.y+cTextBox.height );
        	
        	Core.rectangle( matBoxes2, cBR, cTL, new Scalar( 255, 255, 255 ), 2 );        	
        }
        
        //ds save image
        Highgui.imwrite("10boxes_final.jpg", matBoxes2 );
        
        //ds image center
        //final double dImageCenter = matImageMarked.width( )/2;
        
        /*ds now check the final boxes centers
        for( int i = 0; i < vecTextBoxes.size( )-1; ++i )
        {   
        	//ds tolerance percentages
        	final double dToleranceL = 0.9;
        	final double dToleranceR = 1.1;
        	
        	//ds compute box center
        	final double dCenterX = vecTextBoxes.get( i ).x + vecTextBoxes.get( i ).width/2;
        	
        	//ds check if the box x center is near the central axis of the image
        	if( dToleranceL*dImageCenter > dCenterX || dToleranceR*dImageCenter < dCenterX )
        	{
        		//ds we have to drop the box
        		vecTextBoxes.remove( i );
        		
        		//ds and decrement i
        		--i;
        	}
        }*/
        
        //ds total area
        double dTotalTextArea = 0.0;

        //ds loop over the boxes
        for( Rect cTextBox : vecTextBoxes )
        {
            //ds if the covered area is big enough
            if( 1000 < cTextBox.area( ) )
            {
            	//ds add to total area
            	dTotalTextArea += cTextBox.area( );
            	
            	//ds compute corner points for the rectangle to draw
            	final Point cBR = new Point( cTextBox.x, cTextBox.y );
            	final Point cTL = new Point( cTextBox.x+cTextBox.width, cTextBox.y+cTextBox.height );
            
            	//ds and draw it on the input matrix
            	Core.rectangle( matImageMarked, cBR, cTL, new Scalar( 255, 0, 0 ), 2 );
            }
        }
        
        //ds final image
        Highgui.imwrite("11final.jpg", matImageMarked );
        
        //ds compute text percentage
        final double dTextPercentage = dTotalTextArea/matImageMarked.total( );
        
        //ds log
        System.out.println( "[" + CLogger.getStamp( ) + "]<CImageHandler>(_markTextCanny) text percentage: " + dTextPercentage );

        //ds return the marked image
        return dTextPercentage;
    }
    
    //ds fixes border boxes
    private Vector< Rect > _fixBorderBoxes( final Vector< Rect > p_vecBoxes )
	{
    	//ds vector with border fixed boxes
    	Vector< Rect > vecTextBoxesBorderFixed = new Vector< Rect >( );
    	
        //ds now fix bad boxes (contours connected to the border of the image)
        for( int i = 0; i < p_vecBoxes.size( )-1; ++i )
        {
        	//ds get the current box and the next one (bad access checked)
        	final Rect cTextBox     = p_vecBoxes.get( i );
        	final Rect cTextBoxNext = p_vecBoxes.get( i+1 );
        	
        	//ds if we are at the border and have small height (almost a line)
        	if( 1 == cTextBox.x && 10 > cTextBox.height )
        	{
        		//ds and the next box fulfills the line criteria as well
        		if( 1 == cTextBoxNext.x && 10 > cTextBoxNext.height )
        		{
        			//ds we have to merge these to boxes and add it to the final vector
        			vecTextBoxesBorderFixed.add( new Rect( cTextBoxNext.x, cTextBoxNext.y, cTextBoxNext.width , cTextBox.y-cTextBoxNext.y ) );
        			
        			//ds we can skip the next box
        			++i;
        		}
        		else
        		{
        			//ds TODO: take care of boxes between the invalid boxes
        		}
        	}
        	else
        	{
        		//ds just add the box
        		vecTextBoxesBorderFixed.add( cTextBox );
        	}
        }
        
        //ds always add the last box if its valid
        if( 1 != p_vecBoxes.lastElement( ).x && 10 < p_vecBoxes.lastElement( ).height ){ vecTextBoxesBorderFixed.add( p_vecBoxes.lastElement( ) ); }
        
        //ds return
        return vecTextBoxesBorderFixed;
	}
    
    //ds remove double boxes
    private Vector< Rect > _removeDoubleBoxes( final Vector< Rect > p_vecBoxes )
    {
    	//ds loop over all boxes
        for( int i = 0; i < p_vecBoxes.size( )-1; ++i )
        {
        	//ds get the current and next box
        	final Rect cTextBox     = p_vecBoxes.get( i );
        	final Rect cTextBoxNext = p_vecBoxes.get( i+1 );
        	
        	//ds if they have identical values
        	if( cTextBox.x     == cTextBoxNext.x     && cTextBox.y      == cTextBoxNext.y      &&
        		cTextBox.width == cTextBoxNext.width && cTextBox.height == cTextBoxNext.height )
        	{
        		//ds remove it
        		p_vecBoxes.remove( i+1 );
        	}
        }
        
        //ds and return
        return p_vecBoxes;
    }
    
    //ds remove inner boxes
    private Vector< Rect > _removeInnerBoxes( final Vector< Rect > p_vecBoxes )
    {
    	//ds filtered boxes
    	Vector< Rect > vecBoxesClean = new Vector< Rect >( p_vecBoxes );
    	
        //ds loop over all boxes
        for( Rect cTextBox : p_vecBoxes )
        {
        	//ds loop over all other boxes
            for( int i = 0; i < vecBoxesClean.size( ); ++i )
            {
	        	//ds get the current box
	        	final Rect cTextBoxNext = vecBoxesClean.get( i );
	        	
	        	//ds if its not the same as the outer one
	        	if( cTextBoxNext != cTextBox )
	        	{
		        	//ds check if the next box could possible fit in the current one
		        	if( cTextBox.height > cTextBoxNext.height && cTextBox.width > cTextBoxNext.width )
		        	{
		        		//ds get relative corner distances
		        		final int iPixelDistanceX = cTextBoxNext.x-cTextBox.x;
		        		final int iPixelDistanceY = cTextBoxNext.y-cTextBox.y;
		        		
		        		//ds check the position
		        		if( 0 < iPixelDistanceX && cTextBox.width  > iPixelDistanceX &&
		        			0 < iPixelDistanceY && cTextBox.height > iPixelDistanceY )
		        		{
		        			//ds remove the box
		        			vecBoxesClean.remove( i );
		        		}
		        	}
		        	else
		        	{
		        		//ds get the id of the outer box
		        		final int iBox = vecBoxesClean.indexOf( cTextBox );
		        		
		        		//ds if its still in the vector
		        		if( -1 != iBox )
		        		{
			        		//ds check if the outer box fits in the inner
			        		if( cTextBox.height < cTextBoxNext.height && cTextBox.width < cTextBoxNext.width )
			        		{
				        		//ds get relative corner distances
				        		final int iPixelDistanceX = cTextBox.x-cTextBoxNext.x;
				        		final int iPixelDistanceY = cTextBox.y-cTextBoxNext.y;
				        		
				        		//ds check the position
				        		if( 0 < iPixelDistanceX && cTextBoxNext.width  > iPixelDistanceX &&
				        			0 < iPixelDistanceY && cTextBoxNext.height > iPixelDistanceY )
				        		{
				        			//ds remove the box
				        			vecBoxesClean.remove( iBox );
				        		}
			        		}
		        		}
		        	}
	        	}
	        }
        }
        
        //ds return the clean vector
        return vecBoxesClean;
    }
    
    //ds connects boxes
    private Vector< Rect > _connectBoxes( final Vector< Rect > p_vecBoxes )
	{
    	//ds vector with border fixed boxes
    	Vector< Rect > vecTextBoxesPool = new Vector< Rect >( p_vecBoxes );
    	
    	//ds vector with connected boxes
    	Vector< Rect > vecTextBoxesConnected = new Vector< Rect >( );
    	
    	//ds maximum corner shift between boxes
    	final int iPixelMaximumShiftY = 2;
    	final int iPixelMaximumShiftX = 5;
    	
    	//ds height similarity
    	final double dHeightToleranceL = 0.9;
    	final double dHeightToleranceR = 1.1;
    	
        //ds loop over all boxes
        for( Rect cTextBox : p_vecBoxes )
        {
	        //ds loop over all other remaining boxes
	        for( int i = 0; i < vecTextBoxesPool.size( ); ++i )
	        {
	        	//ds get the next box
	        	final Rect cTextBoxNext = p_vecBoxes.get( i );
	        	
	        	//ds get the id of the outer box
	        	final int iBox = vecTextBoxesPool.indexOf( cTextBox );
	        	
	        	//ds if not the same and not -1
	        	if( cTextBoxNext !=cTextBox && -1 != iBox )
	        	{
		        	//ds if the y shift is not too big
		        	if( iPixelMaximumShiftY >= Math.abs( cTextBoxNext.y-cTextBox.y ) )
		        	{
		        		//ds check if the heights are similiar
		        		if( dHeightToleranceL*cTextBox.height < cTextBoxNext.height && dHeightToleranceR*cTextBox.height > cTextBoxNext.height )
		        		{
		        			//ds get the x distance
		        			final int iPixelShift = cTextBoxNext.x-cTextBox.x;
		        		
			        		//ds check if the next box is to our right
			        		if( 0 < iPixelShift )
			        		{
			        			//ds get relative shift
			        			final int iPixelShiftRelative = cTextBoxNext.x-( cTextBox.x+cTextBox.width );
			        			
			        			//ds if the box is not too far away
				        		if( iPixelMaximumShiftX >= iPixelShiftRelative )
				        		{
				        			//ds get a new box
				        			Rect cTextBoxConnected = new Rect( 0, 0, 0, 0 );
				        			
					        		//ds set the box width
					        		cTextBoxConnected.width = cTextBox.width+cTextBoxNext.width;
					        		
					        		//ds take the bigger height
					        		if( cTextBox.height > cTextBoxNext.height ){ cTextBoxConnected.height = cTextBox.height; }
					        		else                                       { cTextBoxConnected.height = cTextBoxNext.height; }
					        		
					        		//ds take the coordinates from the current box (since we are on the right end)
					        		cTextBoxConnected.x = cTextBox.x;
					        		cTextBoxConnected.y = cTextBox.y;
					        		
					        		System.out.println( "box 1: " + cTextBox );
					        		System.out.println( "box 2: " + cTextBoxNext );
					        		
					        		//ds we now can remove the old boxes
					        		vecTextBoxesPool.remove( i );
					        		vecTextBoxesPool.remove( iBox );
					        		
						        	//ds add the box
					        		vecTextBoxesConnected.add( cTextBoxConnected );
						        	
						        	System.out.println( "added 1" );
				        		}
			        		}
			        		
			        		//ds or to our left
			        		else
			        		{
			        			//ds get relative shift
			        			final int iPixelShiftRelative = cTextBox.x-( cTextBoxNext.x+cTextBoxNext.width );
			        			
				        		//ds if the box is not too far away
				        		if( iPixelMaximumShiftX >= iPixelShiftRelative )
				        		{
				        			//ds get a new box
				        			Rect cTextBoxConnected = new Rect( 0, 0, 0, 0 );
				        			
					        		//ds set the box width
					        		cTextBoxConnected.width = cTextBox.width+cTextBoxNext.width;
					        		
					        		//ds take the bigger height
					        		if( cTextBox.height > cTextBoxNext.height ){ cTextBoxConnected.height = cTextBox.height; }
					        		else                                       { cTextBoxConnected.height = cTextBoxNext.height; }
					        		
					        		//ds take the coordinates from the next box (since we are on the left end)
					        		cTextBoxConnected.x = cTextBoxNext.x;
					        		cTextBoxConnected.y = cTextBoxNext.y;
					        		
					        		System.out.println( "box 1: " + cTextBox );
					        		System.out.println( "box 2: " + cTextBoxNext );
					        		
					        		//ds we now can remove the old boxes
					        		vecTextBoxesPool.remove( i );
					        		vecTextBoxesPool.remove( iBox );
					        		
						        	//ds add the box
					        		vecTextBoxesConnected.add( cTextBoxConnected );
						        	
						        	System.out.println( "added 2" );
				        		}		        			
			        		}
		        		}
		        	}
	        	}
	        }
        }
        
        //ds always add the last box if its valid
        //if( 1 != p_vecBoxes.lastElement( ).x && 10 < p_vecBoxes.lastElement( ).height ){ vecTextBoxesConnected.add( p_vecBoxes.lastElement( ) ); }
        
        //ds merge vectors
        vecTextBoxesPool.addAll( vecTextBoxesConnected );
        
        //ds return
        return vecTextBoxesPool;
	}
    
    //ds compute text area
    private Mat _markTextSimple( final Mat p_matImage )
    {
        //ds allocate new matrix to manipulate
        Mat matImageMarked = p_matImage.clone( );
        
        //ds allocate a new mat (grayscale)
        Mat matImageGray = new Mat( p_matImage.size( ), CvType.CV_8UC1 );
        
        //ds get the grayscale representation
        Imgproc.cvtColor( p_matImage, matImageGray, Imgproc.COLOR_RGB2GRAY );
         
        //ds threshold
        Imgproc.threshold( matImageGray, matImageGray, 245, 255, Imgproc.THRESH_BINARY );
        
        //ds vector with centers
        final Vector< Double > vecTextCenters = new Vector< Double >( );
        
        //ds call the recursive text detector
        _detectTextCenters( matImageGray, 0, matImageGray.rows( ), 0.0, 0, vecTextCenters );
        
        //ds draw the centers
        for( double dRowCenter : vecTextCenters )
        {
            //ds green
            Core.line( matImageMarked, new Point( 0, dRowCenter ), new Point( matImageMarked.width( ), dRowCenter ), new Scalar( 0, 255, 0 ), 1 );
        }
        
        //ds filter the centers vector for the best centers
        final Vector< Double > vecTextCentersFinal = _filterTextCenters( vecTextCenters );
        
        //ds draw the final centers
        for( double dRowCenter : vecTextCentersFinal )
        {
            //ds red
            Core.circle( matImageMarked, new Point( matImageMarked.width( )/2, dRowCenter ), 5, new Scalar( 0, 0, 255 ), -1 );
        }
        
        //ds obtain the text boxes
        final Vector< Rect > vecTextBoxes = _getTextBoxes( matImageGray, vecTextCentersFinal );
        
        //ds text area
        double dTotalTextArea = 0.0;
        
        //ds draw the boxes
        for( Rect cTextBox : vecTextBoxes )
        {
            //ds blue
            Core.rectangle( matImageMarked, cTextBox.tl( ), cTextBox.br( ), new Scalar( 255, 0, 0 ) );
            
            //ds add to area
            dTotalTextArea += cTextBox.area( );
        }
        
        //ds compute text percentage
        final double dTextPercentage = dTotalTextArea/matImageMarked.total( );
        
        //ds log
        System.out.println( "[" + CLogger.getStamp( ) + "]<CImageHandler>(_markTextSimple) text percentage: " + dTextPercentage );
        
        //ds return the marked image
        return matImageMarked;
    }
    
    //ds retrieve text centers
    private final void _detectTextCenters( final Mat p_cImage, 
                                           final int p_iRowStart, 
                                           final int p_iRowEnd, 
                                           final double p_dLastRowCenter, 
                                           final int p_dRecursionDepth, 
                                           final Vector< Double > p_vecCenters )
    {
        //ds abort if max depth is reached or trivial sub mat
        if( 10 == p_dRecursionDepth || p_iRowStart == p_iRowEnd ){ return; }
        
        //ds get total moments for our segment
        final Moments cMoments = Imgproc.moments( p_cImage.submat( new Range( p_iRowStart, p_iRowEnd ), new Range( 0, p_cImage.width( ) ) ) );
        
        //ds get moments
        final double dM01 = cMoments.get_m01( );
        final double dM00 = cMoments.get_m00( );
        final double dM10 = cMoments.get_m10( );
        
        //ds return if 0 (no momentum)
        if( 0 == dM00 ){ return; }
        
        //ds get the centers of mass (and add the offset caused by the row start)
        final double dRowCenter = dM01/dM00 + p_iRowStart;
        final double dColCenter = dM10/dM00;
        
        //ds compute whiteness
        final double dWhiteness = _getWhiteness( p_cImage.submat( new Range( p_iRowStart, p_iRowEnd ), new Range( 0, p_cImage.width( ) ) ) );
        
//        System.out.println( "Center Row at: " + dRowCenter + " [" + p_iRowStart + "-" + p_iRowEnd + "][" + 0 + "-" + p_cImage.width( ) + "]" );
//        System.out.println( "Center Col at: " + dColCenter );
//        System.out.println( "Whiteness: " + dWhiteness );
//        System.out.println( "Last Center at: " + p_dLastRowCenter );
        
        //ds if its near the last centers (1%) and there are enough white pixels
        if(    0.99*p_dLastRowCenter < dRowCenter               &&
                          dRowCenter < 1.01*p_dLastRowCenter    && 
            0.95*p_cImage.width( )/2 < dColCenter               &&
                          dColCenter < 1.05*p_cImage.width( )/2 &&
                                0.25 < dWhiteness               )
        {
            //ds add the center point and return
            p_vecCenters.add( dRowCenter );
            return;
        }
        else
        {
            //ds in compute the available columns to split up
            final int iRows = p_iRowEnd - p_iRowStart;
            
            //ds call the childs
            _detectTextCenters( p_cImage, p_iRowStart          , p_iRowStart + iRows/2 , dRowCenter, p_dRecursionDepth+1, p_vecCenters );
            _detectTextCenters( p_cImage, p_iRowStart + iRows/2, p_iRowEnd             , dRowCenter, p_dRecursionDepth+1, p_vecCenters );
        }
    }
    
    private final Vector< Double > _filterTextCenters( final Vector< Double > p_vecCenters )
    {
        //ds special case for 0 and 1 center
        if( p_vecCenters.isEmpty( ) ){ return new Vector< Double >( ); }
        if( 1 == p_vecCenters.size( ) ){ return p_vecCenters; }
        
        
        //ds sort the centers vector
        Collections.sort( p_vecCenters );
        
        //ds final centers vector
        Vector< Double > vecFinalCenters = new Vector< Double >( );
        
        //ds average center vector
        Vector< Double > VecCurrentCentersCluster = new Vector< Double >( );
        
        //ds we can already add the first center point
        VecCurrentCentersCluster.add( p_vecCenters.firstElement( ) );
        
        //ds loop over all elements
        for( double dCenter : p_vecCenters )
        {
            //ds get center distance
            final double dDistance = Math.abs( dCenter - VecCurrentCentersCluster.lastElement( ) );
            
            //ds check if the centers are close enough
            if( 20 > dDistance )
            {
                //ds add the center to the group
                VecCurrentCentersCluster.add( dCenter );
            }
            else
            {
                //ds the center was too far away and does not belong to the current group - compute mid center for the group and add it
                vecFinalCenters.add( ( VecCurrentCentersCluster.firstElement( ) + VecCurrentCentersCluster.lastElement( ) )/2 );
                
                //ds clear the group vector
                VecCurrentCentersCluster.clear( );
                
                //ds and add this center
                VecCurrentCentersCluster.add( dCenter );
            }
        }
        
        //ds calculate the last final center
        vecFinalCenters.add( ( VecCurrentCentersCluster.firstElement( ) + VecCurrentCentersCluster.lastElement( ) )/2 );        
        
        //ds return the final centers
        return vecFinalCenters;
    }
    
    private final Vector< Rect > _getTextBoxes( final Mat p_matImageGrayscale, final Vector< Double > p_vecFinalCenters )
    {
        //ds allocate rectangle vector
        Vector< Rect > vecTextBoxes = new Vector< Rect >( );
        
        //ds for all centers
        for( double dCenter : p_vecFinalCenters )
        {  
            //ds get center as integer
            int iCenter = ( int )dCenter;
            
            //ds parameters
            int iWidth        = 20;
            int iHeightUp     = 5;
            int iHeightDown   = 5;
            
            //ds initial box to evolve
            Rect cTextBox = new Rect( p_matImageGrayscale.width( )/2-50/2, iCenter-iHeightUp/2, 50, iHeightUp );
            
            //ds center values for the fitting
            int iCurrentYUp    = cTextBox.y-iHeightUp;
            int iCurrentYDown  = cTextBox.y;
            int iCurrentXLeft  = cTextBox.x;
            int iCurrentXRight = cTextBox.x+iWidth/2;
            
            
            //ds whiteness value
            double dWhiteness = 1.0;

            //ds minimum whiteness
            final double dMinimumWhiteness = 0.01;
            
            //ds UP: while we are in the text region
            while( dMinimumWhiteness < dWhiteness )
            {                
                //ds first extend upwards
                Rect cTextBoxFit = new Rect( cTextBox.x, iCurrentYUp, cTextBox.width, iHeightUp );
                
                //ds if possible
                if( 0 < cTextBoxFit.tl( ).x && p_matImageGrayscale.width( ) > cTextBoxFit.tl( ).x && 0 < cTextBoxFit.tl( ).y && p_matImageGrayscale.height( ) > cTextBoxFit.tl( ).y &&
                    0 < cTextBoxFit.br( ).x && p_matImageGrayscale.width( ) > cTextBoxFit.br( ).x && 0 < cTextBoxFit.br( ).y && p_matImageGrayscale.height( ) > cTextBoxFit.br( ).y )
                {            
                    //ds get whiteness
                    dWhiteness = _getWhiteness( p_matImageGrayscale.submat( cTextBoxFit ) );
                    
                    //ds check to extend up - if the whiteness is high enough
                    if( dMinimumWhiteness < dWhiteness )
                    {
                        //ds adjust y position for next check
                        iCurrentYUp -= iHeightUp;
                    }
                }
                else
                {
                    //ds reached end of image
                    break;
                }
            }
            
            //ds reset
            dWhiteness = 1.0;
            
            //ds DOWN: while we are in the text region
            while( dMinimumWhiteness < dWhiteness )
            {
                //ds first extend upwards
                Rect cTextBoxFit = new Rect( cTextBox.x, iCurrentYDown, cTextBox.width, iHeightDown );
                
                if( 0 < cTextBoxFit.tl( ).x && p_matImageGrayscale.width( ) > cTextBoxFit.tl( ).x && 0 < cTextBoxFit.tl( ).y && p_matImageGrayscale.height( ) > cTextBoxFit.tl( ).y &&
                    0 < cTextBoxFit.br( ).x && p_matImageGrayscale.width( ) > cTextBoxFit.br( ).x && 0 < cTextBoxFit.br( ).y && p_matImageGrayscale.height( ) > cTextBoxFit.br( ).y )
                { 
                    //ds get whiteness
                    dWhiteness = _getWhiteness( p_matImageGrayscale.submat( cTextBoxFit ) );
                    
                    //ds check to extend up
                    if( dMinimumWhiteness < dWhiteness )
                    {
                        //ds adjust y position for next check
                        iCurrentYDown += iHeightDown;
                    }
                }
                else
                {
                    //ds reached end of image
                    break;
                }
            }
            
            //ds update text box height settings
            cTextBox.y      = iCurrentYUp;
            cTextBox.height = iCurrentYDown - iCurrentYUp;
            
            //ds check settings
            while( 0                             > cTextBox.y ){ cTextBox.y += 1; };
            while( p_matImageGrayscale.height( ) < cTextBox.y ){ cTextBox.y -= 1; };
            
            //ds reset
            dWhiteness = 1.0;
            
            //ds check to extend left and right
            while( dMinimumWhiteness < dWhiteness )
            {
                //ds get left and right box
                Rect cTextBoxFitLeft  = new Rect( iCurrentXLeft,  cTextBox.y, iWidth/2, cTextBox.height );
                Rect cTextBoxFitRight = new Rect( iCurrentXRight, cTextBox.y, iWidth/2, cTextBox.height );
                
                //ds check borders
                if( 0 < iCurrentXLeft && p_matImageGrayscale.width( ) > iCurrentXRight )
                { 
                    //ds get whiteness
                    dWhiteness = ( _getWhiteness( p_matImageGrayscale.submat( cTextBoxFitLeft ) ) + _getWhiteness( p_matImageGrayscale.submat( cTextBoxFitRight ) ) )/2;
                    
                    //ds check to extend up
                    if( dMinimumWhiteness < dWhiteness )
                    {
                        //ds adjust center points for next check
                        iCurrentXLeft  -= iWidth/2;
                        iCurrentXRight += iWidth/2;
                    }
                }
                else
                {
                    //ds reached end of image
                    break;
                }
            }
            
            //ds apply changes
            cTextBox.width  = iCurrentXRight-iCurrentXLeft;
            cTextBox.x      = p_matImageGrayscale.width( )/2 - cTextBox.width/2;
            
            //ds check limits
            while( 0                             > cTextBox.x ){ cTextBox.x += 1; };
            while( p_matImageGrayscale.width( )  < cTextBox.x ){ cTextBox.x -= 1; };
            
            //ds add it to the vector
            vecTextBoxes.add( cTextBox );
        }
        
        //ds overhand the boxes
        return vecTextBoxes;
    }
    
    //ds computes percentage of white pixels in the image
    private double _getWhiteness( final Mat p_matImage )
    {
        //ds white pixel counter
        int iNumberOfWhitePixels = 0;
        
        //ds loop over the matrix
        for( int r = 0; r < p_matImage.rows( ); ++r )
        {
            for( int c = 0; c < p_matImage.cols( ); ++c )
            {
                //ds if we got a white pixel
                if( 255 == p_matImage.get( r, c)[0] )
                {
                    //ds increase counter
                    ++iNumberOfWhitePixels;
                }
            }
        }
        
        //ds compute percentage and return it
        return ( double )iNumberOfWhitePixels/( p_matImage.rows( )*p_matImage.cols( ) );
    }
    
    //ds get colorspectrum
    private int _getColorSpectrum( final Mat p_matImage )
    {
    	/*final Scalar dMean = Core.mean( p_matImage );
    	
    	MatOfDouble cMean   = new MatOfDouble( );
    	MatOfDouble cStdDev = new MatOfDouble( );
    	
    	Core.meanStdDev( p_matImage, cMean, cStdDev );
    	
    	System.out.println( "mean: " + dMean );
    	System.out.println( "mean: " + cMean.get(1, 0)[0] );
    	System.out.println( "stdd: " + cStdDev.get(1, 0)[0] );*/
    	
    	//Imgproc.Sobel( p_matImage, p_matImage, CvType.CV_8U, 1, 1 );
    	
    	
        //ds log
        //System.out.println( "[" + CLogger.getStamp( ) + "]<CImageHandler>(getImageResized) Color Spectrum: " + vecRGB.size( ) );
        
    	return 0;
    }
    
    /*ds mark text: http://felix.abecassis.me/2011/10/opencv-bounding-box-skew-angle/
    private Mat _markText( final Mat p_matImage )
    {
    	//ds allocate a new mat (grayscale)
    	Mat matImageGray = new Mat( p_matImage.size( ), CvType.CV_8UC1 );
    	
    	//ds get the grayscale representation
    	Imgproc.cvtColor( p_matImage, matImageGray, Imgproc.COLOR_RGB2GRAY );
    	 
    	//ds binarize
    	Imgproc.threshold( matImageGray, matImageGray, 225, 255, Imgproc.THRESH_BINARY );
    	
    	//ds invert colors
    	Core.bitwise_not( matImageGray, matImageGray );
    	
    	//ds structuring element
    	Mat matStructure = Imgproc.getStructuringElement( Imgproc.MORPH_RECT, new Size( 5, 3 ) );
    	
    	//ds erode
    	Imgproc.erode( matImageGray, matImageGray, matStructure );
    	
    	//ds scan the image by handd
    	List< Point > lstPoints = new ArrayList< Point >( );
    	
    	//ds loop over the matrix
    	for( int i = 0; i < matImageGray.height( ); i++ )
    	{
    		for (int j = 0; j < matImageGray.width( ); j++)
    		{
    			//ds if its white
    			if( 255 == matImageGray.get( i, j )[0] )
    			{
    				//ds add the point
    				lstPoints.add( new Point( i,j ) );
    			}
    		}
    	}
    	
    	//ds allocate point mat
    	MatOfPoint2f mat2fImage = new MatOfPoint2f( ); 
    	
    	//ds set it
    	mat2fImage.fromList( lstPoints );
    	
    	//ds get the rotated structure
    	RotatedRect cBox = Imgproc.minAreaRect( mat2fImage );
    	
    	//ds draw the box - allocate vertices
    	Point[] vertices = new Point[4];
    	
    	//ds get the box points
    	cBox.points( vertices );

    	//ds draw all 4 vertices
    	for( int i = 0; i < 4; ++i )
    	{
    		//ds draw for each point
    		Core.line( p_matImage, vertices[i], vertices[(i + 1) % 4], new Scalar( 255, 0, 0 ), 1 );
    	}
    	 
    	return p_matImage;
    }*/
}
