package com.github.mjdev.libaums.fs.fat32;

import com.eclipsesource.json.JsonObject;
import com.github.mjdev.libaums.util.Pair;

import org.junit.runner.RunWith;
import org.xenei.junit.contract.Contract;
import org.xenei.junit.contract.ContractImpl;
import org.xenei.junit.contract.ContractSuite;
import org.xenei.junit.contract.IProducer;


/**
 * Created by magnusja on 03/08/17.
 */
@RunWith(ContractSuite.class)
@ContractImpl(FatFile.class)
public class FatFileTest {

    private IProducer producer = new Fat32FileSystemProducer("https://www.dropbox.com/s/nek7bu08prykkhv/expectedValues.json?dl=1",
            "https://www.dropbox.com/s/3bxngiqmwitlucd/mbr_fat32.img?dl=1");

    @Contract.Inject
    public IProducer<Pair<Fat32FileSystem, JsonObject>> makeFat32FileSystem() {
        return producer;
    }

}