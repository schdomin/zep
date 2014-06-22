function [ ] = plotFigureWithRandomPoints( p_matLearning, p_strUsername, p_strFilename )

%ds font size
uFontSize = 16;

figure( 1 );
plot = plotyy( p_matLearning(:,1), log10( p_matLearning(:,3) ), p_matLearning(:,1), p_matLearning(:,2) );
%grid on;
hold on;
title( [ 'Performance including Random Images (shaded): User [', p_strUsername, ']' ], 'FontSize', uFontSize, 'interpreter', 'Latex' );
xLabel = xlabel( 'Image number', 'FontSize', uFontSize, 'interpreter', 'Latex' );
ylabel(plot(1),'Probability for Like (log10)', 'FontSize', uFontSize, 'interpreter', 'Latex' ) % left y-axis
ylabel(plot(2),'Netto Likes', 'FontSize', uFontSize, 'interpreter', 'Latex' ) % right y-axis

set( xLabel, 'Units', 'Normalized', 'Position', [ 0.5, -0.08, 0]);

%ds draw random point rectangles - determine y height
vecLimitsY = ylim( );
dHeightY = vecLimitsY(2)-vecLimitsY(1);

%ds last random point
uLastRandomPointEnd   = 1;
uLastRandomPointStart = 1;

%ds loop over all points
for u = 1:size( p_matLearning(:,4), 1 )
   
    %ds check if random
    if( 1 == p_matLearning(u,4) )
       
        %ds update random
        uLastRandomPointEnd = u;
        
    elseif( 1 == u-uLastRandomPointEnd )
       
        %ds draw rectangle
        rectangle( 'Position', [ uLastRandomPointStart-1, vecLimitsY(1), uLastRandomPointEnd-uLastRandomPointStart+1, dHeightY ], 'FaceColor',[ 0.95, 0.95, 0.95 ], 'Clipping', 'off', 'LineStyle', ':' );
        
        %ds update start
        uLastRandomPointStart = u;
        
    else
        
        %ds update start
        uLastRandomPointStart = u;        
        
    end
    
end

%ds hold off
hold off;

set( plot, 'FontSize', uFontSize );

saveas( 1, p_strFilename, 'epsc' );

end
