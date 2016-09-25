package com.github.mjdev.libaums.fs;

import java.io.IOException;

/**
 * Created by Yuriy on 25.09.2016.
 */

public interface FatIntf {

    Long[] getChain(long startCluster) throws IOException;

    Long[] alloc(Long[] chain, int numberOfClusters) throws IOException;

    Long[] free(Long[] chain, int numberOfClusters) throws IOException;
}
