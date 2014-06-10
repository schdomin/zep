clear;

%ds load csv data
matLearning1 = csvread( 'users/ChillyOnMyWilly.csv' );
matLearning2 = csvread( 'users/Glaus.csv' );
matLearning3 = csvread( 'users/Isabellinska.csv' );
matLearning4 = csvread( 'users/judas.csv' );
matLearning5 = csvread( 'users/juitto.csv' );
matLearning6 = csvread( 'users/ProProcrastinator.csv' );
matLearning7 = csvread( 'users/themanuuu.csv' );
matLearning8 = csvread( 'users/memyself.csv' );

%ds plot figures
plotFigureWithRandomPoints( matLearning1, 'ChillyOnMyWilly'  , 'users/probability_curve_ChillyOnMyWilly_random.eps' );
plotFigureWithRandomPoints( matLearning3, 'Isabellinska'     , 'users/probability_curve_Isabellinska_random.eps' );
plotFigureWithRandomPoints( matLearning6, 'ProProcrastinator', 'users/probability_curve_ProProcrastinator_random.eps' );
plotFigureWithRandomPoints( matLearning7, 'themanuuu'        , 'users/probability_curve_themanuuu_random.eps' );
plotFigureWithRandomPoints( matLearning8, 'memyself'         , 'users/probability_curve_memyself_random.eps' );
