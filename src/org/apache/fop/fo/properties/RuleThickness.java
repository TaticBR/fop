package org.apache.fop.fo.properties;

import org.apache.fop.fo.expr.PropertyException;
import org.apache.fop.datatypes.PropertyValue;
import org.apache.fop.datatypes.Length;
import org.apache.fop.fo.PropNames;
import org.apache.fop.fo.properties.Property;

public class RuleThickness extends Property  {
    public static final int dataTypes = LENGTH;
    public static final int traitMapping = RENDERING;
    public static final int initialValueType = LENGTH_IT;
    public /**/static/**/ PropertyValue getInitialValue(int property)
        throws PropertyException
    {
        return Length.makeLength
                            (PropNames.RULE_THICKNESS, 1.0d, Length.PT);
    }
    public static final int inherited = COMPUTED;
}

