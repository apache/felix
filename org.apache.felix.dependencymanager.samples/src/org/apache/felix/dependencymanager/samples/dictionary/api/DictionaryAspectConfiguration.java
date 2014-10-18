package org.apache.felix.dependencymanager.samples.dictionary.api;

import java.util.List;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * This interface describes the configuration for our DictionaryAspect component. We are using the bnd metatype
 * annotations, allowing to configure our Dictionary Services from web console.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@OCD(description = "Declare here the list of english words to be added into the default english dictionary")
public interface DictionaryAspectConfiguration {
    @AD(description = "Dictionary aspect words")
    List<String> words();
}
