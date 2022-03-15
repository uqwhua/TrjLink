# TrjLink

Source code for ICDE 2019 paper: [Moving Object Linking Based on Historical Trace](https://ieeexplore.ieee.org/abstract/document/8731507) and TKDE 2020 paper: [Trajectory-Based Spatiotemporal Entity Linking](https://ieeexplore.ieee.org/abstract/document/9250637)

## Problem Description
Given two sets of moving objects with their historical traces （one as data, the other as query), 
we expect to find the top-k candidates for each object in the query set, which are highly possible to the same individual in real life

## Dataset
T-Drive is a publicly available dataset and can be downloaded [here](https://www.microsoft.com/en-us/research/publication/t-drive-trajectory-data-sample/).
A small subset of T-Drive is provided in this project for testing.

## Project structure
    Testing/                                  
    ├── RoadNetworkInfo/NNid2lnglat.csv        -- the road network information used for simple map-matching as the preprosseing of data
    ├── TestData-tdrive/                       -- T-Drive dataset, including 600 objects' one-week data,
                                                  note that some objects could be discarded due to too few data points
    src/                                      
    ├── resources/config.properties           -- the program will read parameters here (modified according to your requirements)
    ├── main/Main.java                        -- the entry of the program
    ├── basic/                                -- the basic geometry defined here
    ├── io/                                   -- read/write file
    ├── signatures/                           -- four types of signature are provided here
    
    libs                                      -- some external library packages that are necessary to be included in the program
    ...                 

## Environment
Tested in CentOS Linux and MacOS Monterey (jdk 17.0.1)

## Citation

If you find our algorithms or the experimental results useful, please kindly cite the following papers:
```
@INPROCEEDINGS{jin2019,
  author={Jin, Fengmei and Hua, Wen and Xu, Jiajie and Zhou, Xiaofang},
  booktitle={2019 IEEE 35th International Conference on Data Engineering (ICDE)}, 
  title={Moving Object Linking Based on Historical Trace}, 
  year={2019},
  pages={1058-1069},
  doi={10.1109/ICDE.2019.00098}
}

@ARTICLE{jin2020,
  author={Jin, Fengmei and Hua, Wen and Zhou, Thomas and Xu, Jiajie and Francia, Matteo and Orowska, Maria and Zhou, Xiaofang},
  journal={IEEE Transactions on Knowledge and Data Engineering}, 
  title={Trajectory-Based Spatiotemporal Entity Linking}, 
  year={2020},
  volume={},
  number={},
  pages={1-1},
  doi={10.1109/TKDE.2020.3036633}
}
```

Please feel free to contact fengmei.jin@uq.edu.au if encountering some unexpected issues in this project.
