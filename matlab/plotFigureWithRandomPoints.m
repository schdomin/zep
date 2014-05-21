function [ ] = plotFigureWithRandomPoints( p_matLearning, p_strUsername, p_strFilename )

figure( 1 );
plot = plotyy( p_matLearning(:,1), log10( p_matLearning(:,3) ), p_matLearning(:,1), p_matLearning(:,2) );
grid on;
hold on;
title( 'Probability including Random Points (grey)' );
legend( p_strUsername );
xlabel( 'datapoint' );
ylabel(plot(1),'probability (log10)') % left y-axis
ylabel(plot(2),'netto likes') % right y-axis

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
        rectangle( 'Position', [ uLastRandomPointStart-1, vecLimitsY(1), uLastRandomPointEnd-uLastRandomPointStart+1, dHeightY ], 'FaceColor',[ 0.85, 0.85, 0.85 ] );
        
        %ds update start
        uLastRandomPointStart = u;
        
    else
        
        %ds update start
        uLastRandomPointStart = u;        
        
    end
    
end

%ds hold off
hold off;

saveas( 1, p_strFilename );

end

