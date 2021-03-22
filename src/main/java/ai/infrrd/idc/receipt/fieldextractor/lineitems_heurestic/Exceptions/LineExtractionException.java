package ai.infrrd.idc.receipt.fieldextractor.lineitems_heurestic.Exceptions;

import org.slf4j.Logger;


public class LineExtractionException extends Exception
{
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger( LineExtractionException.class );

    public LineExtractionException( String message )
    {
        super( message );
    }
}
