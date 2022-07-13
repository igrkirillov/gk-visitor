package ru.x5.gk.visitor;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResultData {

    private final String[] headers;
    private final List<ResultDataRow> rows = new ArrayList<>();

    public synchronized ResultDataRow newRow() {
        ResultDataRow row = new ResultDataRow();
        rows.add(row);
        return row;
    }

    public String[] getHeaders() {
        return headers;
    }

    public synchronized List<ResultDataRow> getRows() {
        return rows;
    }

    public class ResultDataRow {

        public ResultDataRow() {
            colValues = new Object[headers.length];
        }

        private Object[] colValues;

        public void addColValue(String header, Object value) {
            int index = -1;
            for (int i = 0; i < headers.length; ++i) {
                if (headers[i].equals(header)) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                colValues[index] = value;
            } else {
                throw new IllegalArgumentException(header);
            }
        }

        public String toDebugString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < headers.length; ++i) {
                sb.append(headers[i] + "=" + colValues[i] + "; ");
            }
            return sb.toString();
        }

        public Object[] getColValues() {
            return colValues;
        }
    }
}
