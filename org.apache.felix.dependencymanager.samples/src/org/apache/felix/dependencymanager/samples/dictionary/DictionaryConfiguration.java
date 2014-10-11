package org.apache.felix.dependencymanager.samples.dictionary;

import java.util.List;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * This interface describes the configuration for our DictionaryImpl component. We are using the bnd metatype
 * annotations, allowing to configure our Dictionary Services from web console.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@OCD(factory = true, description = "Declare here some Dictionary instances, allowing to instantiates some DictionaryService services for a given dictionary language")
public interface DictionaryConfiguration {
    @AD(description = "Describes the dictionary language", deflt = "en")
    String lang();

    @AD(description = "Declare here the list of words supported by this dictionary.")
    List<String> words();
}
