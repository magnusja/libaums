package com.github.mjdev.libaums.fs.fat16;

import com.eclipsesource.json.JsonObject;
import com.github.mjdev.libaums.util.Pair;

import org.junit.runner.RunWith;
import org.xenei.junit.contract.Contract;
import org.xenei.junit.contract.ContractImpl;
import org.xenei.junit.contract.ContractSuite;
import org.xenei.junit.contract.IProducer;


@RunWith(ContractSuite.class)
@ContractImpl(FatFile.class)
public class Fat16FileTest {

    private IProducer producer = new Fat16FileSystemProducer("https://raw.githubusercontent.com/gopai/iso-store/master/expectedValues.json",
            "https://github.com/gopai/iso-store/raw/master/fat16test.iso.gz");

    @Contract.Inject
    public IProducer<Pair<Fat16FileSystem, JsonObject>> makeFat16FileSystem() {
        return producer;
    }

}