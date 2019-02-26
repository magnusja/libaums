package com.github.mjdev.libaums.fs.fat32;

import com.eclipsesource.json.JsonObject;
import com.github.mjdev.libaums.util.Pair;

import org.junit.runner.RunWith;
import org.xenei.junit.contract.Contract;
import org.xenei.junit.contract.ContractImpl;
import org.xenei.junit.contract.ContractSuite;
import org.xenei.junit.contract.IProducer;

@RunWith(ContractSuite.class)
@ContractImpl(FatFile.class)
public class FatFileTest2 {

    private IProducer producer = new Fat32FileSystemProducer("https://www.dropbox.com/s/9kdizg4lcolhzed/expectedValues_blocksize32K.json?dl=1",
            "https://www.dropbox.com/s/npob26c563ujeyk/mbr_fat32_blocksize32k.img?dl=1");

    @Contract.Inject
    public IProducer<Pair<Fat32FileSystem, JsonObject>> makeFat32FileSystem() {
        return producer;
    }

}