package exceptions;

public final class CZEPConversionException extends CZEPException
{
    //ds constructor
    public CZEPConversionException( String p_strErrorText )
    {
        super( p_strErrorText );
    }
    
    //ds default by inheritance
    private static final long serialVersionUID = 1L;
}
