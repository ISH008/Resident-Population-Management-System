import { regionData } from "element-china-area-data";

const normalizeRegionData = (nodes = []) =>
  nodes.map((province) => ({
    label: province.label,
    value: province.label,
    cities: (province.children || []).map((city) => ({
      label: city.label === "市辖区" || city.label === "县" ? province.label : city.label,
      value: city.label === "市辖区" || city.label === "县" ? province.label : city.label,
      districts: (city.children || []).map((district) => district.label)
    }))
  }));

export const REGION_OPTIONS = normalizeRegionData(regionData);
