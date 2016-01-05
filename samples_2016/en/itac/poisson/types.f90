!
!  ./Poisson/types.f90
!
MODULE types
!

INTEGER:: I__
INTEGER, PARAMETER :: idefault = KIND(I__)

REAL::    R__
INTEGER, PARAMETER :: rdefault = KIND(R__)

DOUBLE PRECISION::  D__
INTEGER, PARAMETER :: ddefault = KIND(D__)

INTEGER, PARAMETER::  INT1 = SELECTED_INT_KIND(1)
INTEGER, PARAMETER::  INT2 = SELECTED_INT_KIND(3)
INTEGER, PARAMETER::  INT4 = SELECTED_INT_KIND(5)
INTEGER, PARAMETER::  INT8 = SELECTED_INT_KIND(10)

INTEGER, PARAMETER::  REAL4 = SELECTED_REAL_KIND(1,1)
INTEGER, PARAMETER::  REAL8 = SELECTED_REAL_KIND(7,1)
INTEGER, PARAMETER::  REAL16= SELECTED_REAL_KIND(16,1)
INTEGER, PARAMETER::  REAL32= -1

END MODULE
