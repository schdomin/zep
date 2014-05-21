function [ ] = plotFigureWithoutRandomPoints( p_matLearning, p_strUsername, p_strFilename )


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
plot = plotyy( vecDataPoints, log10( vecProbabilities ), vecDataPoints, vecNettoLikes );
grid on;
title( 'Probability excluding Random Points' );
legend( p_strUsername );
xlabel( 'datapoint' );
ylabel(plot(1),'probability (log10)') % left y-axis
ylabel(plot(2),'netto likes') % right y-axis

saveas( 1, p_strFilename );

end
