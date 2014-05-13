%ds load csv data
vecFrequencies = csvread( 'frequency.csv' );

%ds sort the vector
vecSorted = sort( vecFrequencies, 'descend' );

%ds plot
figure( 1 );
bar( vecSorted );
%grid on;
title( 'Feature Frequency' );
xlabel( 'feature types' );
ylabel( 'frequency' );
saveas( 1, 'feature_frequency.pdf' );