%ds load csv data
matFeatures = csvread( 'features-2014-04-15.csv' );

%ds plot
figure( 1 );
plot( matFeatures(:,1), matFeatures(:,2), 'blue', matFeatures(:,1), matFeatures(:,3), 'red', matFeatures(:,1), matFeatures(:,1), '-.g' );
grid on;
title( 'Feature Evolvement' );
legend( 'feature types', 'feature number', 'datapoints' );
xlabel( 'datapoints' );
ylabel( 'number' );
saveas( 1, 'feature_density.pdf' );