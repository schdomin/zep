function [ ] = plotFigureWithoutRandomPoints( p_matLearning, p_strUsername, p_strFilename )

%ds font size
uFontSize = 16;
dLineWidth = 2.0;

%ds data holders to be filled (dynamically)
vecDataPoints    = [];
vecProbabilities = [];
vecNettoLikes    = [];

%ds before plotting we have to filter the random points out
for u = 1:size( p_matLearning, 1 )

    %ds if not random
    if( 0 == p_matLearning( u, 4 ) )
       
        %ds add the data
        vecDataPoints    = [ vecDataPoints; p_matLearning( u, 1 ) ];
        vecProbabilities = [ vecProbabilities; p_matLearning( u, 3 ) ];
        vecNettoLikes    = [ vecNettoLikes; p_matLearning( u, 2 ) ];
        
    end
end

figure( 1 );
[ plot, y1, y2 ] = plotyy( vecDataPoints, log10( vecProbabilities ), vecDataPoints, vecNettoLikes );

%ds set line widths
set( y1, 'LineWidth', dLineWidth );
set( y2, 'LineWidth', dLineWidth );

%grid on;
title( [ 'Performance excluding Random Images: User [', p_strUsername, ']' ], 'FontSize', uFontSize, 'interpreter', 'Latex' );
xLabel = xlabel( 'Image number', 'FontSize', uFontSize, 'interpreter', 'Latex' );
ylabel( plot(1),'Probability for Like (log10)', 'FontSize', uFontSize, 'interpreter', 'Latex' ) % left y-axis
ylabel( plot(2),'Netto Likes', 'FontSize', uFontSize, 'interpreter', 'Latex' ) % right y-axis

set( xLabel, 'Units', 'Normalized', 'Position', [ 0.5, -0.08, 0]);

set( plot, 'FontSize', uFontSize );

saveas( 1, p_strFilename, 'epsc' );

end
