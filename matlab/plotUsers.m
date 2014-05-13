%ds load csv data
matLearning1 = csvread( 'users/ChillyOnMyWilly.csv' );
matLearning2 = csvread( 'users/Glaus.csv' );
matLearning3 = csvread( 'users/Isabellinska.csv' );
matLearning4 = csvread( 'users/judas.csv' );
matLearning5 = csvread( 'users/juitto.csv' );
matLearning6 = csvread( 'users/ProProcrastinator.csv' );
matLearning7 = csvread( 'users/themanuuu.csv' );

%ds plot probability curves
figure( 1 );
plot1 = plotyy( matLearning1(:,1), matLearning1(:,3), matLearning1(:,1), matLearning1(:,2) );
grid on;
title( 'Probability' );
legend( 'ChillyOnMyWilly' );
xlabel( 'datapoint' );
ylabel(plot1(1),'probability') % left y-axis
ylabel(plot1(2),'netto likes') % right y-axis
saveas( 1, 'users/probability_curve_ChillyOnMyWilly.pdf' );

figure( 2 );
plot2 = plotyy( matLearning3(:,1), matLearning3(:,3), matLearning3(:,1), matLearning3(:,2)  );
grid on;
title( 'Probability' );
legend( 'Isabellinska' );
xlabel( 'datapoint' );
ylabel(plot2(1),'probability') % left y-axis
ylabel(plot2(2),'netto likes') % right y-axis
saveas( 2, 'users/probability_curve_Isabellinska.pdf' );

figure( 3 );
plot3 = plotyy( matLearning6(:,1), matLearning6(:,3), matLearning6(:,1), matLearning6(:,2)  );
grid on;
title( 'Probability' );
legend( 'ProProcrastinator' );
xlabel( 'datapoint' );
ylabel(plot3(1),'probability') % left y-axis
ylabel(plot3(2),'netto likes') % right y-axis
saveas( 3, 'users/probability_curve_ProProcrastinator.pdf' );

figure( 4 );
plot4 = plotyy( matLearning7(:,1), matLearning7(:,3), matLearning7(:,1), matLearning7(:,2)  );
grid on;
title( 'Probability' );
legend( 'themanuuu' );
xlabel( 'datapoint' );
ylabel(plot4(1),'probability') % left y-axis
ylabel(plot4(2),'netto likes') % right y-axis
saveas( 4, 'users/probability_curve_themanuuu.pdf' );