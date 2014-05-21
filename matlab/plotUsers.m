%ds load csv data
matLearning1 = csvread( 'users/ChillyOnMyWilly.csv' );
matLearning2 = csvread( 'users/Glaus.csv' );
matLearning3 = csvread( 'users/Isabellinska.csv' );
matLearning4 = csvread( 'users/judas.csv' );
matLearning5 = csvread( 'users/juitto.csv' );
matLearning6 = csvread( 'users/ProProcrastinator.csv' );
matLearning7 = csvread( 'users/themanuuu.csv' );
matLearning8 = csvread( 'users/memyself.csv' );

%ds plot probability curves
plotFigureWithoutRandomPoints( matLearning1, 'ChillyOnMyWilly', 'users/probability_curve_ChillyOnMyWilly.pdf' );
plotFigureWithoutRandomPoints( matLearning3, 'Isabellinska', 'users/probability_curve_Isabellinska.pdf' );
plotFigureWithoutRandomPoints( matLearning6, 'ProProcrastinator', 'users/probability_curve_ProProcrastinator.pdf' );
plotFigureWithoutRandomPoints( matLearning7, 'themanuuu', 'users/probability_curve_themanuuu.pdf' );
plotFigureWithoutRandomPoints( matLearning8, 'memyself', 'users/probability_curve_memyself.pdf' );
