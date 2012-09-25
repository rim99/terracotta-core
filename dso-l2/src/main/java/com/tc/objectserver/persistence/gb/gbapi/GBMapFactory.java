package com.tc.objectserver.persistence.gb.gbapi;

import java.util.Collection;

/**
 * @author tim
 */
public interface GBMapFactory {

  <K, V> GBMap<K, V> createMap(Collection<Object> configs);
}
