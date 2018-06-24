package com.powsybl.gse.map;

import java.util.List;

public class DataTest {

    public static void main(String[] args) {
        for (LineGraphic line : RteOpenData.parseLines()) {
            System.out.print(line.getId());
            line.updateBranches();
            List<BranchGraphic> polySegments = line.getBranches();
            System.out.println(" " + line.getSegments().size() + " " + polySegments.size());
        }
    }
}
