package com.spb512.smog.binance.talib;

import com.spb512.smog.binance.dto.IndicatorDto;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class FinStratModelTest {

    @Test
    void calRsi() {
        double[] dClose = new double[]{19145.5, 19144.5, 19095.8, 19101.9, 19097.3, 19119.5, 19114.3, 19132.3, 19164.4, 19175.2, 19179.9, 19191.7, 19163.6, 19164.7, 19124.6, 19096.4, 19129.1, 19111.8, 19126.5, 19140.0, 19135.4, 19117.2, 19094.5, 19099.5, 19106.2, 19109.4, 19115.4, 19107.8, 19110.2, 19099.9, 19115.9, 19134.2, 19105.7, 19066.8, 19055.6, 19066.4, 19099.5, 19128.0, 19122.0, 19132.4, 19146.0, 19108.4, 19096.0, 19107.0, 19110.0, 19105.4, 19126.9, 19134.9, 19132.1, 19142.1, 19130.9, 19113.9, 19141.8, 19141.7, 19117.9, 19144.3, 19143.7, 19163.3, 19159.2, 19152.3, 19163.0, 19136.5, 19144.2, 19150.0, 19146.5, 19133.8, 19119.6, 19109.5, 19103.1, 19115.4, 19139.6, 19103.2, 19108.2, 19103.9, 19141.9, 19131.7, 19150.6, 19146.4, 19135.0, 19150.0, 19122.2, 19040.9, 19102.2, 19085.5, 19082.4, 19090.8, 19083.8, 19091.7, 19093.0, 19124.6, 19136.2, 19064.4, 19087.3, 19071.5, 19092.8, 19088.1, 19061.5, 19090.3, 19080.6, 19051.9, 19051.8, 19027.2, 19035.0, 19004.5, 19026.4, 19000.0, 18994.8, 18979.7, 19001.9, 18989.6, 19019.5, 19012.1, 19006.4, 19000.9, 19002.7, 19008.8, 18987.5, 18997.1, 18993.3, 18992.2, 18952.0, 18977.6, 18956.1, 18962.2, 18997.6, 18979.0, 18992.8, 18992.3, 18970.9, 19003.8, 19003.8, 19028.7, 19033.6, 19021.8, 19028.0, 19017.4, 19038.4, 19057.1, 19076.7, 19076.4, 19099.7, 19094.7, 19101.6, 19075.2, 19060.5, 19054.7, 19060.7, 19018.9, 19011.8, 19000.0, 19016.8, 19028.5, 19027.2, 19026.5, 19027.5, 19043.0, 19034.0, 19040.0, 19025.4, 19015.7, 19025.0, 19024.0, 19032.1, 19022.1, 19024.8, 19025.2, 19051.5, 19053.7, 19065.3, 19033.2, 19035.2, 19036.1, 19007.8, 19037.5, 19020.2, 19010.2, 19150.0, 19030.4, 19035.9, 19033.5, 19079.4, 19037.1, 19032.7, 19030.3, 19015.1, 19022.1, 19084.8, 19070.0, 19078.1, 19089.0, 19054.8, 19069.1, 19058.1, 19070.2, 19070.4, 19089.0, 19051.2, 19050.0, 19091.2, 19068.3, 19085.3, 19081.6, 19071.0, 19084.8, 19074.8, 19070.0, 19080.0, 19071.8, 19079.7, 19081.8, 19131.2, 19135.2, 19165.2, 19173.5, 19146.2, 19167.1, 19154.2, 19146.5, 19154.1, 19165.3, 19183.9, 19207.8, 19169.8, 19179.6, 19159.7, 19148.5, 19125.3, 19135.4, 19125.3, 19115.8, 19140.2, 19129.7, 19134.4, 19153.6, 19137.8, 19118.6, 19112.6, 19126.2, 19114.9, 19120.1, 19119.3, 19071.9, 19105.3, 19098.2, 19074.2, 19091.9, 19161.9, 19108.2, 19117.3, 19116.9, 19125.8, 19108.6, 19092.5, 19036.1, 19111.0, 19079.8, 19090.7, 19098.1, 19089.2, 19096.9, 19097.1, 19101.8, 19077.0, 19055.2, 19060.9, 19049.7, 19024.6, 19030.0, 19047.7, 19058.1, 19056.9, 19064.2, 19057.3, 19053.7, 19074.5, 19076.2, 19085.3, 19073.3, 19120.7, 19127.6, 19160.9, 19200.0, 19142.0, 19140.0, 19103.2, 19109.5, 19132.7, 19120.1, 19129.4, 19101.3, 19129.9, 19121.2, 19115.7, 19106.3, 19089.2, 19096.1, 19092.3, 19097.8, 19090.8, 19100.9, 19116.0, 19113.9, 19138.7, 19146.0, 19137.8, 19123.4, 19144.9, 19129.3, 19144.9, 19106.1, 19096.4, 19089.6, 19095.7, 19120.6, 19120.6, 19105.6, 19086.4, 19076.4, 19078.0, 19067.2, 19062.4, 19057.1, 19066.6, 19083.8, 19089.2, 19074.3, 19085.2, 19071.2, 19084.5, 19075.8, 19073.8, 19083.8, 19096.8, 19095.8, 19089.8, 19093.9, 19140.0, 19199.3, 19125.9, 19133.1, 19117.4, 19123.3, 19116.7, 19114.3, 19118.1, 19101.8, 19110.4, 19092.5, 19091.9, 19088.1, 19095.5, 19126.6, 19091.7, 19124.9, 19116.2, 19099.0, 19099.5, 19110.2, 19109.6, 19106.9, 19108.7, 19099.6, 19103.8, 19090.2, 19080.9, 19094.0, 19093.0, 19093.6, 19099.8, 19098.7, 19093.8, 19103.4, 19107.9, 19130.1, 19227.5, 19227.5, 19172.5, 19137.0, 19231.6, 19161.3, 19139.3, 19129.7, 19116.9, 19101.1, 19104.9, 19039.7, 18966.9, 19000.0, 18964.8, 18950.3, 18944.3, 18964.2, 18984.8, 18976.5, 18967.6, 18931.3, 18923.9, 18940.5, 18984.0, 18935.2, 18918.2, 18930.2, 18928.2, 18937.6, 18995.4, 18953.3, 18963.6, 19041.5, 19041.5, 19080.4, 18917.9, 18945.0, 18931.8, 18932.5, 19108.0, 18944.3, 18952.7, 18940.6, 18885.9, 18900.1, 18903.7, 18932.7, 18902.5, 18923.3, 18922.4, 18913.4, 18928.0, 18901.9, 18910.2, 18914.8, 18890.4, 18907.6, 18960.0, 18939.3, 18955.9, 18938.4, 18930.3, 18938.8, 18920.1, 18940.7, 18945.1, 18948.2, 18984.4, 19100.0, 18965.3, 18953.7, 18926.1, 18936.1, 18959.1, 18958.7, 18961.2, 19082.7, 18943.4, 18964.4, 18960.3, 18965.9, 18961.5, 19082.7, 18966.0, 18949.1, 18947.5, 19034.4, 18940.3, 18950.1, 18932.4, 18979.2, 18944.7, 18953.0, 18954.7, 18972.7, 18971.4, 18977.9, 18990.4, 18984.9, 18993.8, 18986.1, 18978.0, 18978.0, 18977.2, 18966.2, 18961.0, 18955.5, 18958.7, 18946.3, 18926.8, 18937.1, 18951.8, 18963.0, 18958.3, 18952.3, 18961.9, 18965.3, 18969.1, 18953.8, 18960.0, 18957.6, 18956.2, 18969.6, 18950.0, 18955.4};
        FinStratEntity rsi12FinEntity = new FinStratModel().calRsi(dClose, 12);
        double[] dRsi12 = rsi12FinEntity.getRsiReal();
        IndicatorDto indicatorDto = new IndicatorDto();
        indicatorDto.setRsi12(dRsi12[dRsi12.length - 1]);
        System.out.println(Arrays.toString(dRsi12));
    }
}